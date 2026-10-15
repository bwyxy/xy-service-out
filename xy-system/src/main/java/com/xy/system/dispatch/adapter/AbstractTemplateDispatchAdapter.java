package com.xy.system.dispatch.adapter;

import cn.hutool.json.JSONUtil;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.DispatchException;
import com.xy.system.dispatch.adapter.model.ImageUploadRequest;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.adapter.model.PublishedRef;
import com.xy.system.dispatch.adapter.model.StatFetchRequest;
import com.xy.system.dispatch.adapter.model.StatResult;
import com.xy.system.dispatch.support.DispatchHttpClient;
import com.xy.system.dispatch.support.DispatchHttpResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

/**
 * 模板化平台适配器基类
 * 面向没有官方开放接口的平台：发布/采集动作通过平台配置中的接口地址 + 请求模板驱动，
 * 限流、快速重试、错误归一化、结果解析与运营数据采集逻辑均与真实实现一致
 * */
@Slf4j
public abstract class AbstractTemplateDispatchAdapter extends AbstractDispatchPlatformAdapter {

    /** 默认请求体模板，所有字符串占位符以 JSON 字面量形式注入 */
    private static final String DEFAULT_BODY_TEMPLATE =
            "{\"title\":${titleJson},\"content\":${contentJson},\"summary\":${summaryJson},"
                    + "\"tags\":${tagsJson},\"cover\":${coverJson},\"sourceUrl\":${sourceUrlJson},"
                    + "\"original\":${original}}";

    /** 平台默认发布接口地址，返回空表示必须由平台配置提供 */
    protected String defaultPublishUrl() {
        return "";
    }

    protected String defaultPublishMethod() {
        return "POST";
    }

    protected String defaultBodyTemplate() {
        return DEFAULT_BODY_TEMPLATE;
    }

    protected String defaultResultIdPath() {
        return "data.id";
    }

    protected String defaultResultUrlTemplate() {
        return "";
    }

    /** 业务成功码所在字段，返回空表示不做业务码校验 */
    protected String defaultSuccessPath() {
        return "";
    }

    protected String defaultSuccessValue() {
        return "";
    }

    @Override
    protected PublishResult doPublish(PublishContext context) {
        validateCredential(context.getCredential());

        String url = firstNonBlank(context.configString("publishUrl"), defaultPublishUrl());
        if (url == null || url.isBlank()) {
            throw DispatchException.notFound("平台【" + platformName()
                    + "】尚未配置发布接口地址(publishUrl)，请在平台配置的发布规则中补充后重试");
        }
        String method = firstNonBlank(context.configString("publishMethod"), defaultPublishMethod());
        String template = firstNonBlank(context.configString("bodyTemplate"), defaultBodyTemplate());

        DispatchHttpResponse response = request(method, url, buildHeaders(context), buildBody(template, context), context);
        String responseBody = response.getBody();
        if (!response.isSuccess()) {
            return PublishResult.fail(mapError(response.getStatus(), responseBody), buildFailureMessage(response));
        }
        DispatchErrorTypeEnum businessError = detectBusinessError(context, responseBody);
        if (businessError != null) {
            return PublishResult.fail(businessError,
                    "平台返回业务错误：" + DispatchHttpClient.abbreviate(responseBody, 300));
        }

        String idPath = firstNonBlank(context.configString("resultIdPath"), defaultResultIdPath());
        String platformArticleId = (idPath == null || idPath.isBlank()) ? null : readJsonPath(responseBody, idPath);
        return PublishResult.ok(platformArticleId == null ? "" : platformArticleId,
                buildResultUrl(context, platformArticleId));
    }

    @Override
    public boolean supportsStatFetch() {
        return true;
    }

    /**
     * 正文图片迁移到平台图床
     * 需在平台配置中提供 imageUploadUrl，未配置时返回 null 由调用方降级到自有图床
     * */
    @Override
    public boolean supportsImageUpload() {
        return true;
    }

    @Override
    public String uploadContentImage(ImageUploadRequest request) {
        String url = stringOf(request.getConfig(), "imageUploadUrl");
        if (url.isBlank()) {
            return null;
        }
        String field = firstNonBlank(stringOf(request.getConfig(), "imageUploadField"), "file");
        String resultPath = firstNonBlank(stringOf(request.getConfig(), "imageUploadResultPath"), "data.url");
        int timeout = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 15;

        DispatchHttpResponse response = httpClient.postFile(url, buildHeaders(request.getCredential(), request.getConfig()),
                field, request.getBytes(), request.getFileName(), timeout);
        if (!response.isSuccess()) {
            throw new DispatchException(mapError(response.getStatus(), response.getBody()),
                    "上传图片到平台图床失败：HTTP " + response.getStatus());
        }
        return readJsonPath(response.getBody(), resultPath);
    }

    @Override
    public List<StatResult> fetchStats(StatFetchRequest request) {
        validateCredential(request.getCredential());

        String urlTemplate = stringOf(request.getConfig(), "statUrlTemplate");
        if (urlTemplate.isBlank()) {
            log.debug("平台【{}】未配置运营数据接口(statUrlTemplate)，跳过采集", platformName());
            return Collections.emptyList();
        }
        Map<String, String> paths = new LinkedHashMap<>();
        paths.put("view", stringOf(request.getConfig(), "statViewPath"));
        paths.put("like", stringOf(request.getConfig(), "statLikePath"));
        paths.put("comment", stringOf(request.getConfig(), "statCommentPath"));
        paths.put("collect", stringOf(request.getConfig(), "statCollectPath"));
        if (paths.values().stream().allMatch(String::isBlank)) {
            log.debug("平台【{}】未配置运营数据字段路径，跳过采集", platformName());
            return Collections.emptyList();
        }

        if (request.getPlatform() != null) {
            rateLimiter.acquire(platformCode(), request.getPlatform().getRateLimitPerMinute());
        }
        Map<String, String> headers = buildHeaders(request.getCredential(), request.getConfig());
        int timeout = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 15;

        List<StatResult> results = new ArrayList<>();
        for (PublishedRef ref : request.getRefs()) {
            if (ref.getPlatformArticleId() == null || ref.getPlatformArticleId().isBlank()) {
                continue;
            }
            String url = urlTemplate.replace("{id}", ref.getPlatformArticleId());
            try {
                DispatchHttpResponse response = httpClient.execute("GET", url, headers, null, timeout);
                if (!response.isSuccess()) {
                    log.warn("采集平台【{}】文章运营数据失败 articleId={} status={}",
                            platformName(), ref.getPlatformArticleId(), response.getStatus());
                    continue;
                }
                String body = response.getBody();
                StatResult stat = new StatResult();
                stat.setPlatformArticleId(ref.getPlatformArticleId());
                stat.setViewCount(intOf(readJsonPath(body, paths.get("view"))));
                stat.setLikeCount(intOf(readJsonPath(body, paths.get("like"))));
                stat.setCommentCount(intOf(readJsonPath(body, paths.get("comment"))));
                stat.setCollectCount(intOf(readJsonPath(body, paths.get("collect"))));
                results.add(stat);
            } catch (Exception e) {
                log.warn("采集平台【{}】文章运营数据异常 articleId={} err={}",
                        platformName(), ref.getPlatformArticleId(), e.getMessage());
            }
        }
        return results;
    }

    @Override
    public void testConnection(Map<String, String> credential, Map<String, Object> config,
                               BlogDispatchPlatform platform, int timeoutSeconds) {
        validateCredential(credential);
        String probeUrl = stringOf(config, "probeUrl");
        if (probeUrl.isBlank()) {
            return;
        }
        DispatchHttpResponse response = httpClient.execute("GET", probeUrl, buildHeaders(credential, config), null,
                timeoutSeconds > 0 ? timeoutSeconds : 15);
        if (!response.isSuccess()) {
            throw new DispatchException(mapError(response.getStatus(), response.getBody()),
                    "连通性测试失败：HTTP " + response.getStatus() + " "
                            + DispatchHttpClient.abbreviate(response.getBody(), 200));
        }
    }

    /**
     * 根据模板构造请求体，占位符统一以 JSON 字面量注入
     * */
    protected String buildBody(String template, PublishContext context) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("titleJson", jsonValue(context.getTitle()));
        values.put("contentJson", jsonValue(context.getContent()));
        values.put("summaryJson", jsonValue(context.getSummary()));
        values.put("coverJson", jsonValue(context.getCoverImage()));
        values.put("sourceUrlJson", jsonValue(context.getSourceUrl()));
        values.put("accountJson", jsonValue(context.configString("account")));
        values.put("tagsJson", JSONUtil.toJsonStr(context.getTags() == null ? List.of() : context.getTags()));
        values.put("original", String.valueOf(context.configBoolean("original", true)));

        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", Matcher.quoteReplacement(entry.getValue()));
        }
        return result;
    }

    protected Map<String, String> buildHeaders(PublishContext context) {
        return buildHeaders(context.getCredential(), context.getConfig());
    }

    protected Map<String, String> buildHeaders(Map<String, String> credential, Map<String, Object> config) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.putAll(parseHeaders(config));
        if (credential != null) {
            String cookie = credential.get("cookie");
            if (cookie != null && !cookie.isBlank()) {
                headers.put("Cookie", cookie);
            }
            String authorization = credential.get("authorization");
            if (authorization != null && !authorization.isBlank()) {
                headers.put("Authorization", authorization);
            }
            String token = credential.get("token");
            if (token != null && !token.isBlank() && !headers.containsKey("Authorization")) {
                headers.put("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
            }
        }
        return headers;
    }

    /**
     * 解析平台配置中的自定义请求头，支持 JSON 字符串或 Map 两种写法
     * */
    private Map<String, String> parseHeaders(Map<String, Object> config) {
        Map<String, String> headers = new LinkedHashMap<>();
        Object raw = config == null ? null : config.get("headers");
        if (raw == null) {
            return headers;
        }
        if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> {
                if (k != null && v != null) {
                    headers.put(String.valueOf(k), String.valueOf(v));
                }
            });
            return headers;
        }
        String text = String.valueOf(raw).trim();
        if (text.isBlank()) {
            return headers;
        }
        try {
            cn.hutool.json.JSONObject json = JSONUtil.parseObj(text);
            for (String key : json.keySet()) {
                headers.put(key, String.valueOf(json.get(key)));
            }
        } catch (Exception e) {
            log.warn("解析平台自定义请求头失败，已忽略：{}", e.getMessage());
        }
        return headers;
    }

    /**
     * 依据业务成功码判断是否成功，返回 null 表示无业务错误
     * */
    protected DispatchErrorTypeEnum detectBusinessError(PublishContext context, String responseBody) {
        String successPath = firstNonBlank(context.configString("successPath"), defaultSuccessPath());
        if (successPath == null || successPath.isBlank()) {
            return null;
        }
        String expected = firstNonBlank(context.configString("successValue"), defaultSuccessValue());
        if (expected == null || expected.isBlank()) {
            return null;
        }
        String actual = readJsonPath(responseBody, successPath);
        if (actual == null) {
            return null;
        }
        return expected.equalsIgnoreCase(actual.trim()) ? null : mapError(200, responseBody);
    }

    protected String buildResultUrl(PublishContext context, String platformArticleId) {
        String template = firstNonBlank(context.configString("resultUrlTemplate"), defaultResultUrlTemplate());
        if (template == null || template.isBlank()) {
            return "";
        }
        if (template.contains("{id}") && (platformArticleId == null || platformArticleId.isBlank())) {
            return "";
        }
        String url = template;
        if (platformArticleId != null) {
            url = url.replace("{id}", platformArticleId);
        }
        String account = context.configString("account");
        if (account != null) {
            url = url.replace("{account}", account);
        }
        return url;
    }

    protected String buildFailureMessage(DispatchHttpResponse response) {
        return "平台返回 HTTP " + response.getStatus() + "：" + DispatchHttpClient.abbreviate(response.getBody(), 300);
    }

    protected String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    protected String stringOf(Map<String, Object> config, String key) {
        if (config == null) {
            return "";
        }
        Object value = config.get(key);
        return value == null ? "" : String.valueOf(value).trim();
    }

    protected String jsonValue(String value) {
        if (value == null || value.isBlank()) {
            return "null";
        }
        return JSONUtil.toJsonStr(value);
    }

    protected int intOf(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
