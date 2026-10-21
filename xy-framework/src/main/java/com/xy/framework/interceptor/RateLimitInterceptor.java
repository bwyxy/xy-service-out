package com.xy.framework.interceptor;

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
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;

@Component
public class RateLimitInterceptor implements org.springframework.web.servlet.HandlerInterceptor {

    /** 无需限流的路径前缀：接口文档与静态资源不承载业务流量 */
    private static final String[] EXCLUDED_PATH_PREFIXES = {
            "/doc.html", "/swagger-ui", "/swagger-resources", "/v3/api-docs", "/webjars/", "/favicon.ico", "/error"
    };

    private final LettuceBasedProxyManager proxyManager;

    public RateLimitInterceptor(LettuceBasedProxyManager proxyManager) {
        this.proxyManager = proxyManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (isExcluded(uri)) {
            return true;
        }

        String ip = extractIp(request);
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

        // 限流 key 会进入 Redis，显式指定 UTF-8 避免依赖平台默认字符集
        Bucket bucket = proxyManager.builder().build(key.getBytes(StandardCharsets.UTF_8), configSupplier);

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

    private boolean isExcluded(String uri) {
        for (String prefix : EXCLUDED_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析客户端 IP
     *
     * <p>X-Forwarded-For 由客户端可伪造，只有在直连来源是可信代理（本机反向代理）时才采信，
     * 否则一律使用 TCP 层来源地址，避免攻击者伪造 IP 绕过或污染限流计数。</p>
     * */
    private String extractIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String xf = request.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isEmpty()) {
                String first = xf.split(",")[0].trim();
                if (!first.isEmpty()) {
                    return first;
                }
            }
        }

        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    /**
     * 是否为可信代理来源
     *
     * <p>只信任来自内网/回环地址的直接请求：这类请求只可能由我们自己的反向代理
     * （Nginx、网关）或容器网络发出。公网直连请求的 X-Forwarded-For 可被任意伪造，
     * 一律不采信。</p>
     * */
    private boolean isTrustedProxy(String remoteAddr) {
        if (remoteAddr == null) {
            return false;
        }
        if ("::1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            return true;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddr);
            return address.isLoopbackAddress() || address.isSiteLocalAddress();
        } catch (Exception e) {
            return false;
        }
    }
}
