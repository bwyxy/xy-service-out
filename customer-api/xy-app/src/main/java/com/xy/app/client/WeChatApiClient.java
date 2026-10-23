package com.xy.app.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xy.app.response.WeChatApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 微信小程序开放接口客户端
 *
 * <p>只做两件事：拿 access_token（Redis 集中缓存）与下发一次性订阅消息。
 * access_token 必须缓存：并发去取会互相顶掉（微信是「后取的生效」），这是经典事故点。</p>
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeChatApiClient {

    /**
     * access_token 缓存 key 前缀，按 appid 区分
     * */
    private static final String TOKEN_CACHE_PREFIX = "app:wechat:access_token:";

    /**
     * 获取凭证
     * */
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";

    /**
     * 下发订阅消息
     * */
    private static final String SUBSCRIBE_SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";

    /**
     * access_token 无效
     * */
    private static final int ERR_TOKEN_INVALID = 40001;

    /**
     * access_token 过期
     * */
    private static final int ERR_TOKEN_EXPIRED = 42001;

    /**
     * 缓存提前过期的秒数，避免边界上拿到刚失效的凭证
     * */
    private static final long TOKEN_EXPIRE_AHEAD_SECONDS = 300L;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    private final StringRedisTemplate stringRedisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 必须带超时：调度线程不能被微信接口挂死
     * */
    private final RestTemplate restTemplate = buildRestTemplate();

    private static RestTemplate buildRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));

        return new RestTemplate(factory);
    }

    /**
     * 下发订阅消息
     *
     * <p>一次性订阅里「一次授权只能发一条」，多发微信会直接返回错误码，调用方需自行扣额度。</p>
     *
     * @param openid     接收者 openid
     * @param templateId 模板 Id
     * @param page       点击消息后的跳转页，不带前导斜杠；为空则消息无跳转
     * @param data       模板数据，形如 { "thing1": { "value": "…" } }
     * @return 微信响应，errcode 为 0 表示下发成功
     * */
    public WeChatApiResponse sendSubscribeMessage(String openid, String templateId, String page,
                                                  Map<String, Map<String, String>> data) {

        WeChatApiResponse response = doSendSubscribeMessage(getAccessToken(), openid, templateId, page, data);

        // 凭证被别的服务顶掉时会返回 40001/42001，清缓存重取一次再发，不把这次失败算到业务头上
        if (isTokenError(response)) {
            log.warn("微信 access_token 失效，清缓存后重试一次 errcode={}", response == null ? null : response.getErrcode());
            evictToken();
            response = doSendSubscribeMessage(getAccessToken(), openid, templateId, page, data);
        }

        return response;
    }

    /**
     * 取 access_token，优先读 Redis
     * */
    public String getAccessToken() {
        String cacheKey = TOKEN_CACHE_PREFIX + appid;

        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);

            if (cached != null && !cached.isBlank()) {
                return cached;
            }
        } catch (Exception e) {
            // 缓存故障不该中断业务，降级为直接请求微信
            log.warn("读取微信 access_token 缓存失败，改为直接请求 err={}", e.getMessage());
        }

        WeChatApiResponse response = requestAccessToken();
        String token = response.getAccessToken();

        try {
            stringRedisTemplate.opsForValue().set(cacheKey, token,
                    resolveTokenTtlSeconds(response.getExpiresIn()), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写入微信 access_token 缓存失败 err={}", e.getMessage());
        }

        return token;
    }

    /**
     * 清掉缓存的 access_token
     * */
    public void evictToken() {
        try {
            stringRedisTemplate.delete(TOKEN_CACHE_PREFIX + appid);
        } catch (Exception e) {
            log.warn("清除微信 access_token 缓存失败 err={}", e.getMessage());
        }
    }

    /**
     * 调微信接口取新凭证
     * */
    private WeChatApiResponse requestAccessToken() {
        String url = String.format("%s?grant_type=client_credential&appid=%s&secret=%s", TOKEN_URL, appid, secret);

        WeChatApiResponse response = parse(restTemplate.getForObject(url, String.class));

        if (response == null || response.getAccessToken() == null) {
            throw new IllegalStateException("获取微信 access_token 失败: "
                    + (response == null ? "响应为空" : response.getErrcode() + " " + response.getErrmsg()));
        }

        return response;
    }

    /**
     * 凭证缓存时长：比微信给的有效期提前 5 分钟过期，避免边界上拿到刚失效的凭证
     * */
    private long resolveTokenTtlSeconds(Long expiresIn) {
        long seconds = expiresIn == null ? 7200L : expiresIn;

        return Math.max(60L, seconds - TOKEN_EXPIRE_AHEAD_SECONDS);
    }

    /**
     * 真正发一次订阅消息
     *
     * <p>注意：微信网关要求请求带 Content-Length，不接受 Transfer-Encoding: chunked，
     * 否则一律返回 412 Precondition Failed: [no body]。而 Spring Framework 6.1 起
     * RestTemplate 用 Map/对象做请求体时只发 chunked、不带 Content-Length，
     * 所以这里必须自己序列化成字符串并显式设置长度。</p>
     * */
    private WeChatApiResponse doSendSubscribeMessage(String accessToken, String openid, String templateId,
                                                     String page, Map<String, Map<String, String>> data) {
        String url = SUBSCRIBE_SEND_URL + "?access_token=" + accessToken;

        Map<String, Object> body = new HashMap<>();
        body.put("touser", openid);
        body.put("template_id", templateId);
        body.put("lang", "zh_CN");
        body.put("miniprogram_state", "formal");
        body.put("data", data);

        if (page != null && !page.isBlank()) {
            body.put("page", page);
        }

        try {
            String json = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentLength(json.getBytes(StandardCharsets.UTF_8).length);

            String responseBody = restTemplate.postForObject(url, new HttpEntity<>(json, headers), String.class);

            return parse(responseBody);
        } catch (Exception e) {
            // 网络层失败也要让调用方拿到一个可记录的失败结果，走统一的重试逻辑
            log.warn("下发订阅消息请求异常 openid={} err={}", openid, e.getMessage());

            return failure(e);
        }
    }

    /**
     * 组装一个失败响应，交给统一的重试逻辑处理
     * */
    private WeChatApiResponse failure(Exception e) {
        WeChatApiResponse failure = new WeChatApiResponse();
        failure.setErrcode(-1);
        failure.setErrmsg("请求微信接口失败: " + e.getMessage());

        return failure;
    }

    /**
     * 响应体 → 对象，解析失败返回 null
     * */
    private WeChatApiResponse parse(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(body, WeChatApiResponse.class);
        } catch (Exception e) {
            log.warn("解析微信响应失败 err={} body={}", e.getMessage(), body);

            return null;
        }
    }

    /**
     * 是否是凭证失效类错误
     * */
    private boolean isTokenError(WeChatApiResponse response) {
        if (response == null || response.getErrcode() == null) {
            return false;
        }

        return response.getErrcode() == ERR_TOKEN_INVALID || response.getErrcode() == ERR_TOKEN_EXPIRED;
    }
}
