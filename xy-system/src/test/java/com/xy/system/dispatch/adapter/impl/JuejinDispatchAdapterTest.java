package com.xy.system.dispatch.adapter.impl;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.support.DispatchHttpClient;
import com.xy.system.dispatch.support.DispatchHttpResponse;
import com.xy.system.dispatch.support.PlatformRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 掘金适配器单元测试
 * 重点覆盖「创建草稿 → 发布草稿」两步流程、摘要长度硬限制与错误码归一化
 * */
@ExtendWith(MockitoExtension.class)
@DisplayName("掘金适配器")
class JuejinDispatchAdapterTest {

    private static final String DRAFT_CREATE_URL = "https://api.juejin.cn/content_api/v1/article_draft/create";

    private static final String ARTICLE_PUBLISH_URL = "https://api.juejin.cn/content_api/v1/article/publish";

    private static final String DRAFT_OK = "{\"err_no\":0,\"err_msg\":\"success\",\"data\":{\"id\":\"7001\"}}";

    private static final String PUBLISH_OK = "{\"err_no\":0,\"err_msg\":\"success\",\"data\":{\"article_id\":\"7002\"}}";

    /** 20 字，重复 4 次得到 80 字的正文段落 */
    private static final String LONG_PARAGRAPH = "这是一段用于生成掘金摘要的正文首段内容，".repeat(4);

    @Mock
    private DispatchHttpClient httpClient;

    @Mock
    private PlatformRateLimiter rateLimiter;

    private JuejinDispatchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JuejinDispatchAdapter();
        ReflectionTestUtils.setField(adapter, "httpClient", httpClient);
        ReflectionTestUtils.setField(adapter, "rateLimiter", rateLimiter);
    }

    @Test
    @DisplayName("先创建草稿再发布，回传正式文章链接")
    void publish_shouldCreateDraftThenPublish() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 120))
                .thenReturn(new DispatchHttpResponse(200, PUBLISH_OK, 90));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("6809640445233070094"))));

        assertTrue(result.isSuccess());
        assertEquals("7002", result.getPlatformArticleId());
        assertEquals("https://juejin.cn/post/7002", result.getResultUrl());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient, times(2)).execute(eq("POST"), urlCaptor.capture(), any(), bodyCaptor.capture(), anyInt());
        assertEquals(List.of(DRAFT_CREATE_URL, ARTICLE_PUBLISH_URL), urlCaptor.getAllValues());

        JSONObject publishPayload = JSONUtil.parseObj(bodyCaptor.getAllValues().get(1));
        assertEquals("7001", publishPayload.getStr("draft_id"));
        assertFalse(publishPayload.getBool("sync_to_org"));
    }

    @Test
    @DisplayName("创建草稿的请求体按掘金字段规范组装")
    void publish_shouldBuildDraftPayload() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishContext context = buildContext(Map.of("tagIds", List.of("6809640445233070094", "6809640703325372423"),
                "categoryId", "6809637767543259144", "publishAsDraft", true));
        adapter.publish(context);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(eq("POST"), eq(DRAFT_CREATE_URL), any(), bodyCaptor.capture(), anyInt());
        JSONObject payload = JSONUtil.parseObj(bodyCaptor.getValue());

        assertEquals("多平台分发实践", payload.getStr("title"));
        assertEquals("# 多平台分发实践\n正文", payload.getStr("mark_content"));
        // 分类 Id 必须是字符串，不能是数字
        assertEquals("6809637767543259144", payload.getStr("category_id"));
        assertEquals(2, payload.getJSONArray("tag_ids").size());
        assertEquals("6809640445233070094", payload.getJSONArray("tag_ids").getStr(0));
        assertEquals(10, payload.getInt("edit_type"));
        // 掘金 is_original 为 int16，必须传数字 1/0
        assertEquals(1, payload.getInt("is_original"));
        assertFalse(payload.containsKey("cover_image"));
    }

    @Test
    @DisplayName("非原创时 is_original 传数字 0")
    void publish_shouldSendZeroOriginal_whenNotOriginal() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        adapter.publish(buildContext(Map.of("tagIds", List.of("1"), "original", false, "publishAsDraft", true)));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(eq("POST"), eq(DRAFT_CREATE_URL), any(), bodyCaptor.capture(), anyInt());
        assertEquals(0, JSONUtil.parseObj(bodyCaptor.getValue()).getInt("is_original"));
    }

    @Test
    @DisplayName("外部图床封面不随请求发送，避免掘金以参数错误拒绝")
    void publish_shouldDropExternalCover() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishContext context = buildContext(Map.of("tagIds", List.of("1"), "publishAsDraft", true));
        context.setCoverImage("https://file.bwyxy.cn/ef5833dd-banner.jpeg");
        adapter.publish(context);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(eq("POST"), eq(DRAFT_CREATE_URL), any(), bodyCaptor.capture(), anyInt());
        assertFalse(JSONUtil.parseObj(bodyCaptor.getValue()).containsKey("cover_image"));
    }

    @Test
    @DisplayName("掘金自有图床的封面正常附带")
    void publish_shouldKeepJuejinHostedCover() {
        String cover = "https://p3-juejin.byteimg.com/tos-cn-i-k3u1fbpfcp/abc~tplv-k3u1fbpfcp-watermark.image";
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishContext context = buildContext(Map.of("tagIds", List.of("1"), "publishAsDraft", true));
        context.setCoverImage(cover);
        adapter.publish(context);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(eq("POST"), eq(DRAFT_CREATE_URL), any(), bodyCaptor.capture(), anyInt());
        assertEquals(cover, JSONUtil.parseObj(bodyCaptor.getValue()).getStr("cover_image"));
    }

    @Test
    @DisplayName("未配置分类时使用默认分类，标签支持逗号分隔写法")
    void publish_shouldUseDefaultCategoryAndParseCommaTags() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        adapter.publish(buildContext(Map.of("tagIds", "6809640445233070094, 6809640703325372423",
                "publishAsDraft", true)));

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(anyString(), anyString(), any(), bodyCaptor.capture(), anyInt());
        JSONObject payload = JSONUtil.parseObj(bodyCaptor.getValue());
        assertEquals("6809637773935378440", payload.getStr("category_id"));
        assertEquals(2, payload.getJSONArray("tag_ids").size());
    }

    @Test
    @DisplayName("publishAsDraft 为真时只创建草稿，回传草稿编辑地址")
    void publish_shouldStayDraft_whenConfigured() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"), "publishAsDraft", true)));

        assertTrue(result.isSuccess());
        assertEquals("7001", result.getPlatformArticleId());
        assertEquals("https://juejin.cn/editor/drafts/7001", result.getResultUrl());
        verify(httpClient, times(1)).execute(anyString(), anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("发布响应未带文章 Id 时回退使用草稿 Id")
    void publish_shouldFallbackToDraftId_whenArticleIdMissing() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60))
                .thenReturn(new DispatchHttpResponse(200, "{\"err_no\":0,\"err_msg\":\"success\",\"data\":{}}", 40));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"))));

        assertTrue(result.isSuccess());
        assertEquals("7001", result.getPlatformArticleId());
        assertEquals("https://juejin.cn/post/7001", result.getResultUrl());
    }

    @Test
    @DisplayName("摘要不足 50 字时从正文首段提取")
    void publish_shouldDeriveBriefFromContent_whenSummaryShort() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishContext context = buildContext(Map.of("tagIds", List.of("1"), "publishAsDraft", true));
        context.setSummary("太短了");
        context.setContent("# 掘金分发\n\n" + LONG_PARAGRAPH);
        adapter.publish(context);

        assertEquals(LONG_PARAGRAPH, briefOf());
    }

    @Test
    @DisplayName("摘要与正文都不足 50 字时循环补齐到 50 字")
    void publish_shouldPadBrief_whenBothTooShort() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishContext context = buildContext(Map.of("tagIds", List.of("1"), "publishAsDraft", true));
        context.setSummary("太短了");
        context.setContent("");
        adapter.publish(context);

        String brief = briefOf();
        assertEquals(50, brief.length());
        assertTrue(brief.startsWith("太短了"));
    }

    @Test
    @DisplayName("摘要超过 100 字时截断到 100 字")
    void publish_shouldTruncateBrief_whenTooLong() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60));

        PublishContext context = buildContext(Map.of("tagIds", List.of("1"), "publishAsDraft", true));
        context.setSummary("字".repeat(150));
        adapter.publish(context);

        String brief = briefOf();
        assertEquals(100, brief.length());
        assertEquals("字".repeat(100), brief);
    }

    @Test
    @DisplayName("未配置标签时直接失败且不发起请求")
    void publish_shouldFailFast_whenTagIdsMissing() {
        PublishResult result = adapter.publish(buildContext(null));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.PLATFORM_ERROR, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("tagIds"));
        verify(httpClient, never()).execute(anyString(), anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("缺少 Cookie 凭证时直接失败且不发起请求")
    void publish_shouldFailFast_whenCookieMissing() {
        PublishContext context = buildContext(Map.of("tagIds", List.of("1")));
        context.setCredential(new HashMap<>());

        PublishResult result = adapter.publish(context);

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.CREDENTIAL_INVALID, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("cookie"));
        verify(httpClient, never()).execute(anyString(), anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("创建草稿业务码非 0 时归一化为凭证异常")
    void publish_shouldMapCredentialExpired_whenDraftErrNo403() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200,
                        "{\"err_no\":403,\"err_msg\":\"must login\",\"data\":null}", 40));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"))));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.CREDENTIAL_EXPIRED, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("must login"));
        verify(httpClient, times(1)).execute(anyString(), anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("路由不存在时归一化为平台异常")
    void publish_shouldMapPlatformError_whenRouteMissing() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200,
                        "{\"err_no\":2,\"err_msg\":\"请求路由不存在\",\"data\":\"\"}", 30));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"))));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.PLATFORM_ERROR, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("请求路由不存在"));
    }

    @Test
    @DisplayName("发布草稿失败时提示草稿已创建")
    void publish_shouldHintDraftCreated_whenPublishStepFails() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, DRAFT_OK, 60))
                .thenReturn(new DispatchHttpResponse(200,
                        "{\"err_no\":1001,\"err_msg\":\"文章标题重复\",\"data\":null}", 50));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"))));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.DUPLICATE_CONTENT, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("文章标题重复"));
        assertTrue(result.getErrorMsg().contains("草稿已创建"));
    }

    @Test
    @DisplayName("HTTP 429 归一化为限流异常")
    void publish_shouldMapRateLimited_whenStatus429() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(429, "too many requests", 30));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"))));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.RATE_LIMITED, result.getErrorType());
    }

    @Test
    @DisplayName("创建草稿未返回草稿 Id 时失败并提示")
    void publish_shouldFail_whenDraftIdMissing() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"err_no\":0,\"err_msg\":\"success\",\"data\":{}}", 30));

        PublishResult result = adapter.publish(buildContext(Map.of("tagIds", List.of("1"))));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.PLATFORM_ERROR, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("未返回草稿 Id"));
    }

    @Test
    @DisplayName("凭证校验与平台声明正确")
    void meta_shouldDeclareCookieAndMarkdown() {
        assertEquals("juejin", adapter.platformCode());
        assertEquals(List.of("cookie"), adapter.requiredCredentialKeys());
        assertFalse(adapter.supportsImageUpload());
        assertThrows(RuntimeException.class, () -> adapter.validateCredential(Map.of()));
    }

    /**
     * 取出创建草稿请求体中的 brief_content
     * */
    private String briefOf() {
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(eq("POST"), eq(DRAFT_CREATE_URL), any(), bodyCaptor.capture(), anyInt());
        return JSONUtil.parseObj(bodyCaptor.getValue()).getStr("brief_content");
    }

    private PublishContext buildContext(Map<String, Object> extraConfig) {
        PublishContext context = new PublishContext();
        context.setArticleId(1L);
        context.setTitle("多平台分发实践");
        context.setContent("# 多平台分发实践\n正文");
        context.setSourceUrl("https://blog.example.com/article/1");
        Map<String, String> credential = new HashMap<>();
        credential.put("cookie", "sessionid=abc");
        context.setCredential(credential);
        Map<String, Object> config = new HashMap<>();
        config.put("account", "tester");
        if (extraConfig != null) {
            config.putAll(extraConfig);
        }
        context.setConfig(config);
        BlogDispatchPlatform platform = new BlogDispatchPlatform();
        platform.setRateLimitPerMinute(0);
        context.setPlatform(platform);
        context.setTimeoutSeconds(15);
        context.setMaxRetry(1);
        return context;
    }
}
