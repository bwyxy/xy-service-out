package com.todo.framework.interceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

    private final LettuceBasedProxyManager proxyManager;

    public RateLimitInterceptor(LettuceBasedProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = extractIp(request);
        String uri = request.getRequestURI();
        String key = ip + ":" + uri;

        // 根据 URI 设置不同的限流规则
        Supplier<BucketConfiguration> configSupplier;
        if (uri.startsWith("/auth/login")) {
            // 登录接口：每分钟最多 5 次
            configSupplier = () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                    .build();
        } else {
            // 其他接口：每分钟最多 200 次
            configSupplier = () -> BucketConfiguration.builder()
                    .addLimit(Bandwidth.classic(200, Refill.intervally(200, Duration.ofMinutes(1))))
                    .build();
        }

        Bucket bucket = proxyManager.builder().build(key.getBytes(), configSupplier);

        if (!bucket.tryConsume(1)) {
            // 限流触发
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            try (PrintWriter w = response.getWriter()) {
                w.write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                w.flush();
            }
            return false;
        }

        return true;
    }

    private String extractIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isEmpty()) {
            return xf.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return ip == null ? "unknown" : ip;
    }
}
