package com.xy.system.dispatch.adapter;

import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.DispatchException;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.support.DispatchHttpClient;
import com.xy.system.dispatch.support.DispatchHttpResponse;
import com.xy.system.dispatch.support.PlatformRateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Locale;
import java.util.Map;

/**
 * 平台适配器抽象基类
 * 统一收口限流、瞬时异常重试、异常归一化与错误映射，子类只需关注平台协议本身
 * */
@Slf4j
public abstract class AbstractDispatchPlatformAdapter implements DispatchPlatformAdapter {

    /** 瞬时网络异常的内部快速重试次数 */
    private static final int FAST_RETRY_ATTEMPTS = 2;

    private static final long FAST_RETRY_INTERVAL_MS = 300L;

    @Autowired
    protected DispatchHttpClient httpClient;

    @Autowired
    protected PlatformRateLimiter rateLimiter;

    @Override
    public final PublishResult publish(PublishContext context) {
        try {
            if (context.getPlatform() != null) {
                rateLimiter.acquire(platformCode(), context.getPlatform().getRateLimitPerMinute());
            }
            return doPublish(context);
        } catch (DispatchException e) {
            log.warn("分发失败 platform={} articleId={} type={} msg={}",
                    platformCode(), context.getArticleId(), e.getErrorType().getCode(), e.getMessage());
            return PublishResult.fail(e.getErrorType(), e.getMessage());
        } catch (Exception e) {
            log.error("分发出现未预期异常 platform={} articleId={}", platformCode(), context.getArticleId(), e);
            return PublishResult.fail(DispatchErrorTypeEnum.UNKNOWN, "分发异常：" + rootMessage(e));
        }
    }

    /**
     * 平台具体的发布逻辑
     * */
    protected abstract PublishResult doPublish(PublishContext context);

    /**
     * 带瞬时网络异常快速重试的请求入口
     * */
    protected DispatchHttpResponse request(String method, String url, Map<String, String> headers,
                                           String body, PublishContext context) {
        int attempts = Math.max(1, Math.min(FAST_RETRY_ATTEMPTS, Math.max(1, context.getMaxRetry())));
        DispatchException lastError = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return httpClient.execute(method, url, headers, body, context.getTimeoutSeconds());
            } catch (DispatchException e) {
                if (e.getErrorType() != DispatchErrorTypeEnum.NETWORK_ERROR || attempt == attempts) {
                    throw e;
                }
                lastError = e;
                sleep(FAST_RETRY_INTERVAL_MS * attempt);
            }
        }
        throw lastError == null ? DispatchException.networkError("请求失败", null) : lastError;
    }

    /**
     * 把平台返回的 HTTP 状态码与响应体归一化为分发错误类型
     * */
    protected DispatchErrorTypeEnum mapError(int status, String body) {
        String text = body == null ? "" : body.toLowerCase(Locale.ROOT);
        if (status == 429 || containsAny(text, "too many", "rate limit", "限流", "请求过于频繁", "操作太频繁", "frequent")) {
            return DispatchErrorTypeEnum.RATE_LIMITED;
        }
        if (containsAny(text, "未登录", "请登录", "登录已失效", "重新登录", "please login", "unauthorized",
                "invalid token", "token expired", "session expired", "credential expired")) {
            return DispatchErrorTypeEnum.CREDENTIAL_EXPIRED;
        }
        if (containsAny(text, "违规", "审核不通过", "敏感", "禁止发布", "content violates", "forbidden word")) {
            return DispatchErrorTypeEnum.CONTENT_VIOLATION;
        }
        if (containsAny(text, "重复", "已存在", "duplicate", "already exists")) {
            return DispatchErrorTypeEnum.DUPLICATE_CONTENT;
        }
        if (status == 413 || containsAny(text, "too large", "过长", "超出限制", "exceed", "payload too large")) {
            return DispatchErrorTypeEnum.CONTENT_TOO_LARGE;
        }
        if (status == 401) {
            return DispatchErrorTypeEnum.CREDENTIAL_EXPIRED;
        }
        if (status == 403) {
            return DispatchErrorTypeEnum.CREDENTIAL_INVALID;
        }
        return DispatchErrorTypeEnum.PLATFORM_ERROR;
    }

    /**
     * 校验必填凭证是否存在
     * */
    protected void requireCredentials(Map<String, String> credential, String... keys) {
        StringBuilder missing = new StringBuilder();
        for (String key : keys) {
            String value = credential == null ? null : credential.get(key);
            if (value == null || value.isBlank()) {
                if (missing.length() > 0) {
                    missing.append("、");
                }
                missing.append(key);
            }
        }
        if (missing.length() > 0) {
            throw DispatchException.credentialInvalid("平台【" + platformName() + "】缺少必填凭证：" + missing);
        }
    }

    /**
     * 解析 JSON 中的字段（支持 a.b.c 形式的简单路径）
     * */
    protected String readJsonPath(String body, String path) {
        if (body == null || body.isBlank() || path == null || path.isBlank()) {
            return null;
        }
        try {
            cn.hutool.json.JSONObject json = cn.hutool.json.JSONUtil.parseObj(body);
            Object value = json.getByPath(path);
            return value == null ? null : String.valueOf(value);
        } catch (Exception e) {
            log.warn("解析平台响应失败 path={} err={}", path, e.getMessage());
            return null;
        }
    }

    /**
     * 取第一个非空白字符串，均空白时返回第二个
     * */
    protected String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    protected boolean containsAny(String text, String... keywords) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    protected String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
