package com.xy.system.dispatch.adapter.impl;

import cn.hutool.json.JSONArray;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 掘金平台适配器
 * 掘金未提供公开的官方发布接口，此处按其站点内部接口实现真实发布动作：
 * 凭证为浏览器 Cookie，正文以 Markdown 写入 mark_content，
 * 流程为「创建草稿 → 发布草稿」两步，publishAsDraft 为真时仅停留在草稿态
 * */
@Slf4j
@Component
public class JuejinDispatchAdapter extends AbstractDispatchPlatformAdapter {

    /** 创建草稿接口 */
    private static final String DRAFT_CREATE_URL = "https://api.juejin.cn/content_api/v1/article_draft/create";

    /** 发布草稿接口 */
    private static final String ARTICLE_PUBLISH_URL = "https://api.juejin.cn/content_api/v1/article/publish";

    private static final String RESULT_URL_PREFIX = "https://juejin.cn/post/";

    private static final String DRAFT_EDIT_URL_PREFIX = "https://juejin.cn/editor/drafts/";

    private static final String DEFAULT_CATEGORY_ID = "6809637773935378440";

    /** 掘金要求摘要不少于 50 字，不足时创建草稿会被直接拒绝 */
    private static final int MIN_BRIEF_LENGTH = 50;

    /** 掘金摘要上限 100 字 */
    private static final int MAX_BRIEF_LENGTH = 100;

    @Override
    public String platformCode() {
        return DispatchPlatformEnum.JUEJIN.getCode();
    }

    @Override
    public String platformName() {
        return DispatchPlatformEnum.JUEJIN.getName();
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

        List<String> tagIds = resolveTagIds(context);
        if (tagIds.isEmpty()) {
            return PublishResult.fail(DispatchErrorTypeEnum.PLATFORM_ERROR,
                    "掘金要求至少配置一个标签，请在发布默认规则中补充 tagIds");
        }
        String brief = resolveBriefContent(context);
        Map<String, String> headers = buildHeaders(context);
        DispatchHttpResponse draftResponse = request("POST",
                firstNonBlank(context.configString("publishUrl"), DRAFT_CREATE_URL),
                headers, buildDraftPayload(context, brief, tagIds).toString(), context);

        JSONObject draftJson = parseBody(draftResponse);
        if (draftJson == null) {
            return PublishResult.fail(mapError(draftResponse.getStatus(), draftResponse.getBody()),
                    "掘金创建草稿失败，HTTP " + draftResponse.getStatus() + "："
                            + DispatchHttpClient.abbreviate(draftResponse.getBody(), 300));
        }
        PublishResult draftError = rejectIfError(draftJson, "创建草稿");
        if (draftError != null) {
            return draftError;
        }

        String draftId = firstNonBlank(readJsonPath(draftResponse.getBody(), "data.id"),
                readJsonPath(draftResponse.getBody(), "data.draft_id"));
        if (draftId == null || draftId.isBlank()) {
            return PublishResult.fail(DispatchErrorTypeEnum.PLATFORM_ERROR,
                    "掘金创建草稿未返回草稿 Id："
                            + DispatchHttpClient.abbreviate(draftResponse.getBody(), 200));
        }

        if (context.configBoolean("publishAsDraft", false)) {
            return PublishResult.ok(draftId, DRAFT_EDIT_URL_PREFIX + draftId);
        }
        return publishDraft(context, headers, draftId);
    }

    /**
     * 发布草稿，拿到正式文章 Id 后回传访问链接
     * */
    private PublishResult publishDraft(PublishContext context, Map<String, String> headers, String draftId) {
        JSONObject payload = new JSONObject();
        payload.set("draft_id", draftId);
        payload.set("sync_to_org", context.configBoolean("syncToOrg", false));

        DispatchHttpResponse response = request("POST",
                firstNonBlank(context.configString("articlePublishUrl"), ARTICLE_PUBLISH_URL),
                headers, payload.toString(), context);

        JSONObject json = parseBody(response);
        if (json == null) {
            return PublishResult.fail(mapError(response.getStatus(), response.getBody()),
                    "掘金发布草稿失败，HTTP " + response.getStatus() + "："
                            + DispatchHttpClient.abbreviate(response.getBody(), 300) + draftHint(draftId));
        }
        PublishResult error = rejectIfError(json, "发布草稿");
        if (error != null) {
            return PublishResult.fail(error.getErrorType(), error.getErrorMsg() + draftHint(draftId));
        }

        String articleId = firstNonBlank(readJsonPath(response.getBody(), "data.article_id"),
                readJsonPath(response.getBody(), "data.articleId"));
        if (articleId == null || articleId.isBlank()) {
            articleId = firstNonBlank(readJsonPath(response.getBody(), "data.id"), draftId);
        }
        return PublishResult.ok(articleId, RESULT_URL_PREFIX + articleId);
    }

    /**
     * 组装创建草稿的请求体
     * */
    private JSONObject buildDraftPayload(PublishContext context, String brief, List<String> tagIds) {
        JSONObject payload = new JSONObject();
        payload.set("title", context.getTitle());
        payload.set("brief_content", brief);
        payload.set("mark_content", context.getContent() == null ? "" : context.getContent());
        payload.set("category_id", firstNonBlank(context.configString("categoryId"), DEFAULT_CATEGORY_ID));
        payload.set("tag_ids", tagIds);
        payload.set("edit_type", context.configInt("editType", 10));
        // 掘金侧 is_original 为 int16 类型，1 表示原创、0 表示非原创，传布尔值会被反序列化拒绝
        payload.set("is_original", context.configBoolean("original", true) ? 1 : 0);
        String originalLink = context.configString("originalLink");
        if (originalLink != null && !originalLink.isBlank()) {
            payload.set("original_link", originalLink);
        }
        String cover = context.getCoverImage();
        if (cover != null && isJuejinHosted(cover)) {
            payload.set("cover_image", cover);
        }
        return payload;
    }

    /**
     * 掘金要求封面图片必须来自其自有图床，外部图床地址会被参数校验以「参数错误」拒绝，
     * 因此仅当封面地址已属于掘金域名时才附带，否则留空由平台自行取正文首图
     * */
    private boolean isJuejinHosted(String coverUrl) {
        String lower = coverUrl.toLowerCase();
        return lower.contains("byteimg.com") || lower.contains("juejin.cn") || lower.contains("juejin.im");
    }

    private Map<String, String> buildHeaders(PublishContext context) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Referer", "https://juejin.cn/");
        headers.put("Origin", "https://juejin.cn");
        String cookie = context.credential("cookie");
        if (!cookie.isBlank()) {
            headers.put("Cookie", cookie);
        }
        String xsrfToken = context.credential("x-xsrf-token");
        if (xsrfToken.isBlank()) {
            xsrfToken = context.credential("xsrfToken");
        }
        if (!xsrfToken.isBlank()) {
            headers.put("x-xsrf-token", xsrfToken);
        }
        return headers;
    }

    /**
     * 生成符合掘金长度要求的摘要
     * 过短时改用正文首个有效段落，仍不足则用标题兜底并循环补齐，避免创建草稿被平台拒绝
     * */
    private String resolveBriefContent(PublishContext context) {
        String brief = context.getSummary() == null ? "" : context.getSummary().trim();
        if (brief.length() < MIN_BRIEF_LENGTH) {
            String fromContent = firstPlainParagraph(context.getContent());
            if (fromContent.length() > brief.length()) {
                brief = fromContent;
            }
        }
        if (brief.isBlank() && context.getTitle() != null) {
            brief = context.getTitle().trim();
        }
        if (brief.length() > MAX_BRIEF_LENGTH) {
            return brief.substring(0, MAX_BRIEF_LENGTH);
        }
        if (brief.isEmpty()) {
            return brief;
        }
        StringBuilder builder = new StringBuilder(brief);
        while (builder.length() < MIN_BRIEF_LENGTH) {
            builder.append(brief, 0, Math.min(MIN_BRIEF_LENGTH - builder.length(), brief.length()));
        }
        return builder.toString();
    }

    /**
     * 从 Markdown 正文中取第一条有效正文段落并去掉基础标记
     * */
    private String firstPlainParagraph(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        for (String raw : content.split("\n")) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(">") || line.startsWith("```")
                    || line.startsWith("|") || line.startsWith("-") || line.startsWith("*")
                    || line.startsWith("!")) {
                continue;
            }
            String plain = line.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "")
                    .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")
                    .replaceAll("[*`_>#]", "")
                    .trim();
            if (plain.length() >= 10) {
                return plain;
            }
        }
        return "";
    }

    /**
     * 解析掘金标签 Id 列表，支持 JSON 数组或英文逗号分隔两种配置写法
     * */
    private List<String> resolveTagIds(PublishContext context) {
        Object raw = context.getConfig() == null ? null : context.getConfig().get("tagIds");
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof Collection<?> collection) {
            return collection.stream().filter(Objects::nonNull).map(String::valueOf)
                    .map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        String text = String.valueOf(raw).trim();
        if (text.isBlank()) {
            return List.of();
        }
        if (text.startsWith("[")) {
            try {
                JSONArray array = JSONUtil.parseArray(text);
                return array.stream().filter(Objects::nonNull).map(String::valueOf)
                        .map(String::trim).filter(item -> !item.isBlank()).toList();
            } catch (Exception e) {
                log.warn("解析掘金标签配置失败，按逗号分隔处理 err={}", e.getMessage());
            }
        }
        return Arrays.stream(text.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    /**
     * HTTP 状态非成功或响应体无法解析为 JSON 时返回 null
     * */
    private JSONObject parseBody(DispatchHttpResponse response) {
        if (!response.isSuccess()) {
            return null;
        }
        try {
            return JSONUtil.parseObj(response.getBody());
        } catch (Exception e) {
            log.warn("解析掘金响应失败 err={}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验掘金业务码 err_no，返回非空表示失败
     * */
    private PublishResult rejectIfError(JSONObject json, String action) {
        int errNo = json.getInt("err_no", 0);
        if (errNo == 0) {
            return null;
        }
        String errMsg = json.getStr("err_msg");
        return PublishResult.fail(mapJuejinError(errNo, errMsg),
                "掘金" + action + "返回错误 " + errNo + "：" + errMsg);
    }

    /**
     * 草稿已创建但发布失败时提示用户可在草稿箱找到内容
     * */
    private String draftHint(String draftId) {
        return "（草稿已创建，Id=" + draftId + "，可在掘金草稿箱查看）";
    }

    private DispatchErrorTypeEnum mapJuejinError(int errNo, String errMsg) {
        if (errNo == 401 || errNo == 403) {
            return DispatchErrorTypeEnum.CREDENTIAL_EXPIRED;
        }
        String text = errMsg == null ? "" : errMsg;
        if (containsAny(text, "未登录", "登录", "失效", "must login")) {
            return DispatchErrorTypeEnum.CREDENTIAL_EXPIRED;
        }
        if (containsAny(text, "频繁", "限流", "稍后", "too many")) {
            return DispatchErrorTypeEnum.RATE_LIMITED;
        }
        if (containsAny(text, "违规", "审核", "敏感", "禁止")) {
            return DispatchErrorTypeEnum.CONTENT_VIOLATION;
        }
        if (containsAny(text, "重复", "已存在", "duplicate")) {
            return DispatchErrorTypeEnum.DUPLICATE_CONTENT;
        }
        if (containsAny(text, "长度", "超出", "过长", "larger", "至少")) {
            return DispatchErrorTypeEnum.CONTENT_TOO_LARGE;
        }
        return DispatchErrorTypeEnum.PLATFORM_ERROR;
    }
}
