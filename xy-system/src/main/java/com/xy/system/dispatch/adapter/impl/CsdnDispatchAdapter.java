package com.xy.system.dispatch.adapter.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.enums.DispatchPlatformEnum;
import com.xy.system.dispatch.adapter.AbstractDispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.support.DispatchHttpClient;
import com.xy.system.dispatch.support.DispatchHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CSDN 平台适配器
 * CSDN 未提供官方开放接口，此处按其编辑器实际调用的接口实现真实发布动作：
 * 凭证为浏览器 Cookie，请求需额外携带 x-ca-key / x-ca-nonce / x-ca-signature 签名头，
 * 签名算法为 HMAC-SHA256（对 method、accept、content-type、排序后的 x-ca-* 头与请求路径拼接串加密后 Base64），
 * 其中 appKey 与 appSecret 为 CSDN 前端内置的固定常量，nonce 每次请求随机生成
 * */
@Slf4j
@Component
public class CsdnDispatchAdapter extends AbstractDispatchPlatformAdapter {

    /** 编辑器使用的 bizapi 网关地址 */
    private static final String BIZ_API_BASE = "https://bizapi.csdn.net";

    /** Markdown 编辑器保存/发布接口路径 */
    private static final String SAVE_ARTICLE_PATH = "/blog-console-api/v3/mdeditor/saveArticle";

    /** CSDN 前端内置的应用标识与密钥 */
    private static final String APP_KEY = "203803574";

    private static final String APP_SECRET = "9znpamsyl2c7cdrr9sas0le9vbc3r6ba";

    /** 参与签名的请求头名称，顺序固定 */
    private static final String SIGNATURE_HEADERS = "x-ca-key,x-ca-nonce";

    /** 参与签名的固定取值，必须与实际请求头保持一致 */
    private static final String SIGN_ACCEPT = "*/*";

    private static final String SIGN_CONTENT_TYPE = "application/json";

    /** 编辑器来源，部分接口会校验 */
    private static final String EDITOR_ORIGIN = "https://editor.csdn.net";

    /** 保存为草稿 / 直接发布时 status 字段的取值 */
    private static final int STATUS_PUBLISH = 0;

    private static final int STATUS_DRAFT = 2;

    /** CSDN 标签数量上限 */
    private static final int MAX_TAG_COUNT = 5;

    private static final String RESULT_URL_TEMPLATE = "https://blog.csdn.net/%s/article/details/%s";

    @Override
    public String platformCode() {
        return DispatchPlatformEnum.CSDN.getCode();
    }

    @Override
    public String platformName() {
        return DispatchPlatformEnum.CSDN.getName();
    }

    @Override
    public DispatchContentFormat contentFormat() {
        return DispatchContentFormat.MARKDOWN;
    }

    @Override
    public List<String> requiredCredentialKeys() {
        return List.of("cookie");
    }

    @Override
    public void validateCredential(Map<String, String> credential) {
        requireCredentials(credential, "cookie");
    }

    @Override
    protected PublishResult doPublish(PublishContext context) {
        validateCredential(context.getCredential());

        String url = firstNonBlank(context.configString("publishUrl"), BIZ_API_BASE + SAVE_ARTICLE_PATH);
        String nonce = UUID.randomUUID().toString();
        String body = buildPayload(context).toString();

        DispatchHttpResponse response = request("POST", url, buildHeaders(context, url, nonce), body, context);
        if (!response.isSuccess()) {
            return PublishResult.fail(mapCsdnError(response.getStatus(), response.getBody()),
                    "CSDN 返回 HTTP " + response.getStatus() + "："
                            + DispatchHttpClient.abbreviate(response.getBody(), 300));
        }
        return parsePublishResult(context, response.getBody());
    }

    /**
     * 构造发布请求体
     * */
    private JSONObject buildPayload(PublishContext context) {
        String content = context.getContent() == null ? "" : context.getContent();
        boolean asDraft = context.configBoolean("publishAsDraft", false);
        String articleType = firstNonBlank(context.configString("type"), "original");

        JSONObject payload = new JSONObject();
        payload.set("title", context.getTitle());
        payload.set("markdowncontent", content);
        payload.set("content", content);
        payload.set("is_new", 1);
        payload.set("source", "pc_mdeditor");
        payload.set("status", asDraft ? STATUS_DRAFT : STATUS_PUBLISH);
        payload.set("pubStatus", asDraft ? "draft" : "publish");
        payload.set("readType", firstNonBlank(context.configString("readType"), "public"));
        payload.set("type", articleType);
        String summary = context.getSummary();
        if (summary != null && !summary.isBlank()) {
            payload.set("Description", summary);
        }
        String categories = context.configString("categories");
        if (categories != null && !categories.isBlank()) {
            payload.set("categories", categories);
        }
        String tags = joinTags(context.getTags());
        if (!tags.isBlank()) {
            payload.set("tags", tags);
        }
        // 转载/翻译时才需要原文地址
        if (!"original".equalsIgnoreCase(articleType)) {
            String originalUrl = firstNonBlank(context.configString("originalUrl"), context.getSourceUrl());
            if (originalUrl != null && !originalUrl.isBlank()) {
                payload.set("original_url", originalUrl);
            }
        }
        // 覆盖已有草稿时由发布规则提供文章 Id
        String existingId = context.configString("articleId");
        if (existingId != null && !existingId.isBlank()) {
            payload.set("id", existingId);
        }
        return payload;
    }

    /**
     * 构造带签名头的请求头
     * */
    private Map<String, String> buildHeaders(PublishContext context, String url, String nonce) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", SIGN_ACCEPT);
        headers.put("Content-Type", SIGN_CONTENT_TYPE);
        headers.put("Origin", EDITOR_ORIGIN);
        headers.put("Referer", EDITOR_ORIGIN + "/");
        headers.put("Cookie", context.credential("cookie"));
        headers.put("x-ca-key", APP_KEY);
        headers.put("x-ca-nonce", nonce);
        headers.put("x-ca-signature-headers", SIGNATURE_HEADERS);
        headers.put("x-ca-signature",
                sign("POST", SIGN_ACCEPT, SIGN_CONTENT_TYPE, pathOf(url), nonce));
        return headers;
    }

    /**
     * 计算 CSDN 请求签名
     * 待签名串为：method、accept、Content-MD5(空)、Content-Type、Date(空)、x-ca-* 头、请求路径，
     * 各段以换行分隔，加密算法为 HMAC-SHA256，结果 Base64 编码
     * */
    String sign(String method, String accept, String contentType, String path, String nonce) {
        String message = buildSignMessage(method, accept, contentType, path, nonce);
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("计算 CSDN 请求签名失败：" + e.getMessage(), e);
        }
    }

    /**
     * 拼接待签名串，字段顺序与分隔符需与 CSDN 前端严格一致
     * */
    String buildSignMessage(String method, String accept, String contentType, String path, String nonce) {
        return method + "\n"
                + accept + "\n"
                + "\n"
                + contentType + "\n"
                + "\n"
                + "x-ca-key:" + APP_KEY + "\n"
                + "x-ca-nonce:" + nonce + "\n"
                + path;
    }

    /**
     * 解析发布结果
     * */
    private PublishResult parsePublishResult(PublishContext context, String body) {
        if (body == null || body.isBlank()) {
            return PublishResult.ok("", "");
        }
        String trimmed = body.trim();
        // 部分场景接口直接返回布尔值
        if (!trimmed.startsWith("{")) {
            if ("false".equalsIgnoreCase(trimmed)) {
                return PublishResult.fail(DispatchErrorTypeEnum.PLATFORM_ERROR, "CSDN 返回发布失败：" + trimmed);
            }
            return PublishResult.ok("", "");
        }

        JSONObject json;
        try {
            json = JSONUtil.parseObj(trimmed);
        } catch (Exception e) {
            return PublishResult.fail(DispatchErrorTypeEnum.PLATFORM_ERROR,
                    "CSDN 返回内容无法解析：" + DispatchHttpClient.abbreviate(trimmed, 200));
        }
        Integer code = json.getInt("code");
        if (code != null && code != 200) {
            String message = firstNonBlank(json.getStr("msg"), json.getStr("message"));
            return PublishResult.fail(mapCsdnError(200, body), "CSDN 返回错误 " + code + "：" + message);
        }

        String articleId = firstNonBlank(json.getByPath("data.article_id") == null ? null
                        : String.valueOf(json.getByPath("data.article_id")),
                json.getByPath("data.id") == null ? null : String.valueOf(json.getByPath("data.id")));
        articleId = firstNonBlank(articleId, json.getStr("data"));
        if (articleId == null || articleId.isBlank()) {
            return PublishResult.ok("", "");
        }
        return PublishResult.ok(articleId, buildResultUrl(context, articleId));
    }

    /**
     * 回传平台文章访问链接
     * 草稿尚未公开，回传编辑器地址方便直接续写发布；缺少用户名配置时返回空串
     * */
    private String buildResultUrl(PublishContext context, String articleId) {
        if (context.configBoolean("publishAsDraft", false)) {
            return EDITOR_ORIGIN + "/md/?articleId=" + articleId;
        }
        String account = firstNonBlank(context.configString("account"), context.configString("userName"));
        if (account == null || account.isBlank()) {
            log.warn("未配置 CSDN 用户名(account)，无法回传文章访问链接 articleId={}", articleId);
            return "";
        }
        return String.format(RESULT_URL_TEMPLATE, account.trim(), articleId);
    }

    /**
     * 归一化 CSDN 错误提示
     * */
    private DispatchErrorTypeEnum mapCsdnError(int status, String body) {
        String text = body == null ? "" : body.toLowerCase(java.util.Locale.ROOT);
        if (text.contains("x-ca-key") || text.contains("signature") || text.contains("签名")) {
            return DispatchErrorTypeEnum.PLATFORM_ERROR;
        }
        return mapError(status, body);
    }

    private String pathOf(String url) {
        try {
            String path = URI.create(url).getPath();
            return path == null || path.isBlank() ? SAVE_ARTICLE_PATH : path;
        } catch (Exception e) {
            log.warn("解析 CSDN 发布地址失败，按默认路径处理 url={}", url);
            return SAVE_ARTICLE_PATH;
        }
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return tags.stream().filter(tag -> tag != null && !tag.isBlank())
                .map(String::trim).limit(MAX_TAG_COUNT).collect(java.util.stream.Collectors.joining(","));
    }
}
