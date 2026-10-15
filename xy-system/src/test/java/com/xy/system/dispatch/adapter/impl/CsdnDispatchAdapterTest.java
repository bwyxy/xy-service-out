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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CSDN 适配器单元测试
 * 重点覆盖签名串拼装（对照公开的算法说明逐字符校验）与发布结果的解析分支
 * */
@ExtendWith(MockitoExtension.class)
@DisplayName("CSDN 适配器")
class CsdnDispatchAdapterTest {

    /** CSDN 前端内置密钥，签名用例独立实现一遍以便交叉验证 */
    private static final String APP_SECRET = "9znpamsyl2c7cdrr9sas0le9vbc3r6ba";

    private static final String SAVE_ARTICLE_PATH = "/blog-console-api/v3/mdeditor/saveArticle";

    private static final String SAVE_ARTICLE_URL = "https://bizapi.csdn.net" + SAVE_ARTICLE_PATH;

    private static final String FIXED_NONCE = "eff837eb-901e-403f-9ed3-d17d0227b16b";

    @Mock
    private DispatchHttpClient httpClient;

    @Mock
    private PlatformRateLimiter rateLimiter;

    private CsdnDispatchAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CsdnDispatchAdapter();
        ReflectionTestUtils.setField(adapter, "httpClient", httpClient);
        ReflectionTestUtils.setField(adapter, "rateLimiter", rateLimiter);
    }

    @Test
    @DisplayName("待签名串按 CSDN 规范逐段拼装")
    void buildSignMessage_shouldMatchCsdnSpec() {
        String message = adapter.buildSignMessage("POST", "*/*", "application/json", SAVE_ARTICLE_PATH, FIXED_NONCE);

        String expected = "POST\n"
                + "*/*\n"
                + "\n"
                + "application/json\n"
                + "\n"
                + "x-ca-key:203803574\n"
                + "x-ca-nonce:" + FIXED_NONCE + "\n"
                + SAVE_ARTICLE_PATH;
        assertEquals(expected, message);
    }

    @Test
    @DisplayName("签名结果为 HMAC-SHA256 后 Base64，且与独立实现一致")
    void sign_shouldEqualIndependentHmac() throws Exception {
        String signature = adapter.sign("POST", "*/*", "application/json", SAVE_ARTICLE_PATH, FIXED_NONCE);

        String message = "POST\n*/*\n\napplication/json\n\nx-ca-key:203803574\n"
                + "x-ca-nonce:" + FIXED_NONCE + "\n" + SAVE_ARTICLE_PATH;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String expected = Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));

        assertEquals(expected, signature);
        assertEquals(32, Base64.getDecoder().decode(signature).length);
    }

    @Test
    @DisplayName("同一 nonce 签名稳定，不同 nonce 签名不同")
    void sign_shouldBeDeterministicAndNonceSensitive() {
        String first = adapter.sign("POST", "*/*", "application/json", SAVE_ARTICLE_PATH, FIXED_NONCE);
        String second = adapter.sign("POST", "*/*", "application/json", SAVE_ARTICLE_PATH, FIXED_NONCE);
        String other = adapter.sign("POST", "*/*", "application/json", SAVE_ARTICLE_PATH,
                "d73db41b-e7bd-493b-8fe3-3a6995a422b3");

        assertEquals(first, second);
        assertNotEquals(first, other);
    }

    @Test
    @DisplayName("发布成功：回传文章 Id 与访问链接，请求头签名与实际 nonce 一致")
    void publish_shouldReturnUrl_whenSuccess() throws Exception {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200,
                        "{\"code\":200,\"msg\":\"success\",\"data\":{\"article_id\":123456}}", 120));

        PublishResult result = adapter.publish(buildContext(null));

        assertTrue(result.isSuccess());
        assertEquals("123456", result.getPlatformArticleId());
        assertEquals("https://blog.csdn.net/tester/article/details/123456", result.getResultUrl());

        ArgumentCaptor<Map<String, String>> headerCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).execute(eq("POST"), eq(SAVE_ARTICLE_URL), headerCaptor.capture(), anyString(), anyInt());
        Map<String, String> headers = headerCaptor.getValue();
        assertEquals("203803574", headers.get("x-ca-key"));
        assertEquals("x-ca-key,x-ca-nonce", headers.get("x-ca-signature-headers"));
        assertEquals("session=abc", headers.get("Cookie"));
        assertEquals("*/*", headers.get("Accept"));
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("https://editor.csdn.net", headers.get("Origin"));
        assertEquals(adapter.sign("POST", "*/*", "application/json", SAVE_ARTICLE_PATH, headers.get("x-ca-nonce")),
                headers.get("x-ca-signature"));
    }

    @Test
    @DisplayName("请求体字段按配置组装")
    void publish_shouldBuildPayloadFromContext() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":200,\"data\":{\"article_id\":9}}", 80));

        PublishContext context = buildContext(null);
        context.getConfig().put("categories", "后端");
        context.getConfig().put("type", "original");
        context.setSummary("摘要内容");
        context.setTags(List.of("掘金", "后端", "掘金", "Java", "Spring", "多余标签"));
        adapter.publish(context);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(anyString(), anyString(), any(), bodyCaptor.capture(), anyInt());
        JSONObject payload = JSONUtil.parseObj(bodyCaptor.getValue());

        assertEquals("多平台分发实践", payload.getStr("title"));
        assertEquals("# 多平台分发实践\n正文", payload.getStr("markdowncontent"));
        assertEquals("# 多平台分发实践\n正文", payload.getStr("content"));
        assertEquals("摘要内容", payload.getStr("Description"));
        assertEquals("后端", payload.getStr("categories"));
        assertEquals("pc_mdeditor", payload.getStr("source"));
        assertEquals(1, payload.getInt("is_new"));
        assertEquals(0, payload.getInt("status"));
        assertEquals("publish", payload.getStr("pubStatus"));
        assertEquals("public", payload.getStr("readType"));
        assertEquals("original", payload.getStr("type"));
        // 标签最多 5 个，逗号拼接（去重由内容适配层负责）
        assertEquals("掘金,后端,掘金,Java,Spring", payload.getStr("tags"));
        assertFalse(payload.containsKey("original_url"));
        assertFalse(payload.containsKey("id"));
    }

    @Test
    @DisplayName("发布规则 publishAsDraft 为真时保存为草稿")
    void publish_shouldSaveAsDraft_whenConfigured() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":200,\"data\":{\"id\":\"88\"}}", 60));

        PublishContext context = buildContext(Map.of("publishAsDraft", true));
        PublishResult result = adapter.publish(context);

        assertTrue(result.isSuccess());
        assertEquals("88", result.getPlatformArticleId());
        // 草稿未公开，回传编辑器地址而非详情页地址
        assertEquals("https://editor.csdn.net/md/?articleId=88", result.getResultUrl());

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(anyString(), anyString(), any(), bodyCaptor.capture(), anyInt());
        JSONObject payload = JSONUtil.parseObj(bodyCaptor.getValue());
        assertEquals(2, payload.getInt("status"));
        assertEquals("draft", payload.getStr("pubStatus"));
    }

    @Test
    @DisplayName("转载类型附带原文地址")
    void publish_shouldCarryOriginalUrl_whenRepost() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":200,\"data\":{\"article_id\":7}}", 50));

        PublishContext context = buildContext(Map.of("type", "repost"));
        adapter.publish(context);

        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(httpClient).execute(anyString(), anyString(), any(), bodyCaptor.capture(), anyInt());
        JSONObject payload = JSONUtil.parseObj(bodyCaptor.getValue());
        assertEquals("repost", payload.getStr("type"));
        assertEquals("https://blog.example.com/article/1", payload.getStr("original_url"));
    }

    @Test
    @DisplayName("业务码非 200 时返回失败并带上平台提示")
    void publish_shouldFail_whenBusinessCodeNotOk() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":400,\"msg\":\"文章标题重复\"}", 40));

        PublishResult result = adapter.publish(buildContext(null));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.DUPLICATE_CONTENT, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("文章标题重复"));
    }

    @Test
    @DisplayName("签名相关报错归一化为平台异常")
    void publish_shouldMapSignatureError_whenCaKeyMissing() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":403,\"msg\":\"X-Ca-Key is not exist\"}", 30));

        PublishResult result = adapter.publish(buildContext(null));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.PLATFORM_ERROR, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("X-Ca-Key is not exist"));
    }

    @Test
    @DisplayName("HTTP 429 归一化为限流异常")
    void publish_shouldMapRateLimited_whenStatus429() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(429, "too many requests", 30));

        PublishResult result = adapter.publish(buildContext(null));

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.RATE_LIMITED, result.getErrorType());
    }

    @Test
    @DisplayName("缺少 Cookie 凭证时直接失败且不发起请求")
    void publish_shouldFailFast_whenCookieMissing() {
        PublishContext context = buildContext(null);
        context.setCredential(new HashMap<>());

        PublishResult result = adapter.publish(context);

        assertFalse(result.isSuccess());
        assertEquals(DispatchErrorTypeEnum.CREDENTIAL_INVALID, result.getErrorType());
        assertTrue(result.getErrorMsg().contains("cookie"));
        verify(httpClient, never()).execute(anyString(), anyString(), any(), anyString(), anyInt());
    }

    @Test
    @DisplayName("接口返回布尔文本时按成功处理")
    void publish_shouldTreatBooleanBodyAsSuccess() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "true", 20));

        PublishResult result = adapter.publish(buildContext(null));

        assertTrue(result.isSuccess());
        assertEquals("", result.getPlatformArticleId());
    }

    @Test
    @DisplayName("未配置用户名时不回传访问链接")
    void publish_shouldSkipResultUrl_whenAccountMissing() {
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":200,\"data\":{\"article_id\":55}}", 20));

        PublishContext context = buildContext(null);
        context.getConfig().remove("account");
        PublishResult result = adapter.publish(context);

        assertTrue(result.isSuccess());
        assertEquals("55", result.getPlatformArticleId());
        assertEquals("", result.getResultUrl());
    }

    @Test
    @DisplayName("发布地址可在规则中覆盖，签名使用覆盖后的路径")
    void publish_shouldUseConfiguredPublishUrl() {
        String customUrl = "https://bizapi.csdn.net/blog-console-api/v1/postedit/saveArticle";
        when(httpClient.execute(anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(new DispatchHttpResponse(200, "{\"code\":200,\"data\":{\"article_id\":1}}", 20));

        adapter.publish(buildContext(Map.of("publishUrl", customUrl)));

        ArgumentCaptor<Map<String, String>> headerCaptor = ArgumentCaptor.forClass(Map.class);
        verify(httpClient).execute(eq("POST"), eq(customUrl), headerCaptor.capture(), anyString(), anyInt());
        Map<String, String> headers = headerCaptor.getValue();
        assertEquals(adapter.sign("POST", "*/*", "application/json",
                "/blog-console-api/v1/postedit/saveArticle", headers.get("x-ca-nonce")),
                headers.get("x-ca-signature"));
    }

    @Test
    @DisplayName("凭证与正文格式声明正确")
    void meta_shouldDeclareCookieAndMarkdown() {
        assertEquals("csdn", adapter.platformCode());
        assertEquals(List.of("cookie"), adapter.requiredCredentialKeys());
        assertNotNull(adapter.platformName());
    }

    private PublishContext buildContext(Map<String, Object> extraConfig) {
        PublishContext context = new PublishContext();
        context.setArticleId(1L);
        context.setTitle("多平台分发实践");
        context.setContent("# 多平台分发实践\n正文");
        context.setSourceUrl("https://blog.example.com/article/1");
        Map<String, String> credential = new HashMap<>();
        credential.put("cookie", "session=abc");
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
