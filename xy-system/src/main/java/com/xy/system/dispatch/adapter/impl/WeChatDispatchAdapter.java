package com.xy.system.dispatch.adapter.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.enums.DispatchPlatformEnum;
import com.xy.system.dispatch.DispatchException;
import com.xy.system.dispatch.adapter.AbstractDispatchPlatformAdapter;
import com.xy.system.dispatch.adapter.model.ImageUploadRequest;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.support.DispatchHttpClient;
import com.xy.system.dispatch.support.DispatchHttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信公众平台适配器（官方接口真实实现）
 * 走微信官方开放接口：access_token 获取（Redis 缓存）→ 正文图片上传（media/uploadimg）
 * → 封面图上传为永久缩略图素材（material/add_material）→ 新建草稿（cgi-bin/draft/add）
 * 注意：微信草稿创建后没有公开访问链接，需在公众号后台发布后才有 URL，因此结果链接为空
 * */
@Slf4j
@Component
public class WeChatDispatchAdapter extends AbstractDispatchPlatformAdapter {

    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    private static final String UPLOAD_IMG_URL = "https://api.weixin.qq.com/cgi-bin/media/uploadimg";

    private static final String ADD_THUMB_URL = "https://api.weixin.qq.com/cgi-bin/material/add_material";

    private static final String DRAFT_ADD_URL = "https://api.weixin.qq.com/cgi-bin/draft/add";

    private static final String TOKEN_CACHE_PREFIX = "dispatch:wechat:access_token:";

    private final StringRedisTemplate redisTemplate;

    public WeChatDispatchAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String platformCode() {
        return DispatchPlatformEnum.WECHAT.getCode();
    }

    @Override
    public String platformName() {
        return DispatchPlatformEnum.WECHAT.getName();
    }

    @Override
    public DispatchContentFormat contentFormat() {
        return DispatchContentFormat.HTML;
    }

    @Override
    public List<String> requiredCredentialKeys() {
        return List.of("appId", "appSecret");
    }

    @Override
    public void validateCredential(Map<String, String> credential) {
        requireCredentials(credential, "appId", "appSecret");
    }

    @Override
    public boolean supportsImageUpload() {
        return true;
    }

    @Override
    protected PublishResult doPublish(PublishContext context) {
        validateCredential(context.getCredential());
        int timeout = context.getTimeoutSeconds() > 0 ? context.getTimeoutSeconds() : 15;
        String token = accessToken(context.getCredential(), timeout);

        String cover = context.getCoverImage();
        if (cover == null || cover.isBlank()) {
            throw new DispatchException(DispatchErrorTypeEnum.CONTENT_VIOLATION,
                    "微信公众平台草稿必须提供封面图，请先为文章设置封面后重试");
        }
        String thumbMediaId = uploadThumb(token, cover, timeout);

        JSONObject article = new JSONObject();
        article.set("title", context.getTitle());
        article.set("author", context.configString("author") == null ? "" : context.configString("author"));
        article.set("digest", context.getSummary() == null ? "" : context.getSummary());
        article.set("content", context.getContent() == null ? "" : context.getContent());
        article.set("content_source_url", context.getSourceUrl() == null ? "" : context.getSourceUrl());
        article.set("thumb_media_id", thumbMediaId);
        article.set("need_open_comment", context.configInt("needOpenComment", 0));
        article.set("only_fans_can_comment", context.configInt("onlyFansCanComment", 0));

        JSONObject payload = new JSONObject();
        payload.set("articles", new JSONArray().put(article));

        String url = firstNonBlank(context.configString("publishUrl"), DRAFT_ADD_URL) + "?access_token=" + token;
        DispatchHttpResponse response = request("POST", url, Map.of(), payload.toString(), context);
        if (!response.isSuccess()) {
            return PublishResult.fail(mapError(response.getStatus(), response.getBody()),
                    "微信返回 HTTP " + response.getStatus() + "：" + DispatchHttpClient.abbreviate(response.getBody(), 300));
        }
        JSONObject json = parseBody(response.getBody(), "创建微信草稿");
        int errCode = json.getInt("errcode", 0);
        if (errCode != 0) {
            return PublishResult.fail(mapWeChatError(errCode),
                    "微信返回错误 " + errCode + "：" + json.getStr("errmsg"));
        }
        String mediaId = json.getStr("media_id");
        return PublishResult.ok(mediaId == null ? "" : mediaId, "");
    }

    @Override
    public String uploadContentImage(ImageUploadRequest request) {
        int timeout = request.getTimeoutSeconds() > 0 ? request.getTimeoutSeconds() : 15;
        String token = accessToken(request.getCredential(), timeout);
        DispatchHttpResponse response = httpClient.postFile(UPLOAD_IMG_URL + "?access_token=" + token,
                new LinkedHashMap<>(), "media", request.getBytes(), request.getFileName(), timeout);
        if (!response.isSuccess()) {
            throw new DispatchException(mapError(response.getStatus(), response.getBody()),
                    "上传图片到微信图床失败：HTTP " + response.getStatus());
        }
        JSONObject json = parseBody(response.getBody(), "上传图片到微信图床");
        int errCode = json.getInt("errcode", 0);
        if (errCode != 0) {
            throw new DispatchException(mapWeChatError(errCode),
                    "上传图片到微信图床失败：" + errCode + " " + json.getStr("errmsg"));
        }
        String url = json.getStr("url");
        if (url == null || url.isBlank()) {
            throw DispatchException.platformError("上传图片到微信图床失败：响应中无图片地址");
        }
        return url;
    }

    @Override
    public void testConnection(Map<String, String> credential, Map<String, Object> config,
                               BlogDispatchPlatform platform, int timeoutSeconds) {
        validateCredential(credential);
        accessToken(credential, timeoutSeconds > 0 ? timeoutSeconds : 15);
    }

    /**
     * 获取 access_token，优先读取 Redis 缓存，缓存未命中再请求微信接口
     * */
    private String accessToken(Map<String, String> credential, int timeoutSeconds) {
        requireCredentials(credential, "appId", "appSecret");
        String appId = credential.get("appId").trim();
        String appSecret = credential.get("appSecret").trim();
        String cacheKey = TOKEN_CACHE_PREFIX + appId;
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        } catch (Exception e) {
            log.warn("读取微信 access_token 缓存失败，改为直接请求 err={}", e.getMessage());
        }

        String url = TOKEN_URL + "?grant_type=client_credential&appid=" + appId + "&secret=" + appSecret;
        DispatchHttpResponse response = httpClient.get(url, Map.of(), timeoutSeconds);
        if (!response.isSuccess()) {
            throw new DispatchException(mapError(response.getStatus(), response.getBody()),
                    "获取微信 access_token 失败：HTTP " + response.getStatus());
        }
        JSONObject json = parseBody(response.getBody(), "获取微信 access_token");
        int errCode = json.getInt("errcode", 0);
        if (errCode != 0) {
            throw new DispatchException(mapWeChatError(errCode),
                    "获取微信 access_token 失败：" + errCode + " " + json.getStr("errmsg"));
        }
        String token = json.getStr("access_token");
        if (token == null || token.isBlank()) {
            throw DispatchException.platformError("获取微信 access_token 失败：响应中无 access_token");
        }
        long expiresIn = json.getLong("expires_in", 7200L);
        try {
            redisTemplate.opsForValue().set(cacheKey, token, Math.max(60L, expiresIn - 300L), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入微信 access_token 缓存失败 err={}", e.getMessage());
        }
        return token;
    }

    /**
     * 把封面图上传为永久缩略图素材，返回 thumb_media_id
     * */
    private String uploadThumb(String token, String coverUrl, int timeoutSeconds) {
        byte[] bytes = httpClient.download(coverUrl, timeoutSeconds);
        String fileName = "cover" + guessExtension(coverUrl);
        DispatchHttpResponse response = httpClient.postFile(ADD_THUMB_URL + "?access_token=" + token + "&type=thumb",
                new LinkedHashMap<>(), "media", bytes, fileName, timeoutSeconds);
        if (!response.isSuccess()) {
            throw new DispatchException(mapError(response.getStatus(), response.getBody()),
                    "上传微信封面图失败：HTTP " + response.getStatus());
        }
        JSONObject json = parseBody(response.getBody(), "上传微信封面图");
        int errCode = json.getInt("errcode", 0);
        if (errCode != 0) {
            throw new DispatchException(mapWeChatError(errCode),
                    "上传微信封面图失败：" + errCode + " " + json.getStr("errmsg"));
        }
        String mediaId = json.getStr("media_id");
        if (mediaId == null || mediaId.isBlank()) {
            throw DispatchException.platformError("上传微信封面图失败：响应中无 media_id");
        }
        return mediaId;
    }

    private JSONObject parseBody(String body, String action) {
        try {
            return JSONUtil.parseObj(body);
        } catch (Exception e) {
            throw DispatchException.platformError(action + "失败：平台返回内容无法解析，"
                    + DispatchHttpClient.abbreviate(body, 200));
        }
    }

    /**
     * 微信业务错误码映射，便于前端给出明确提示
     * */
    private DispatchErrorTypeEnum mapWeChatError(int errCode) {
        return switch (errCode) {
            case 40001, 40014, 42001 -> DispatchErrorTypeEnum.CREDENTIAL_EXPIRED;
            case 40013, 40125, 40164 -> DispatchErrorTypeEnum.CREDENTIAL_INVALID;
            case 45009, 45011, 45029 -> DispatchErrorTypeEnum.RATE_LIMITED;
            case 45002, 45003 -> DispatchErrorTypeEnum.CONTENT_TOO_LARGE;
            case 87013, 87014 -> DispatchErrorTypeEnum.CONTENT_VIOLATION;
            default -> DispatchErrorTypeEnum.PLATFORM_ERROR;
        };
    }

    private String guessExtension(String url) {
        String path = url.split("\\?")[0];
        int dot = path.lastIndexOf('.');
        if (dot < 0) {
            return ".jpg";
        }
        String extension = path.substring(dot);
        return extension.length() > 5 ? ".jpg" : extension;
    }
}
