package com.xy.blog.service;

import com.xy.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 访问统计防刷服务
 */
@Service
@RequiredArgsConstructor
public class VisitAntiSpamService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String IP_LIMIT_PREFIX = "blog:visit:limit:ip:";
    private static final String VISITOR_LIMIT_PREFIX = "blog:visit:limit:visitor:";

    /**
     * 单 IP 每分钟最大请求数
     */
    private static final int IP_LIMIT_PER_MINUTE = 120;

    /**
     * 单访客每分钟最大请求数
     */
    private static final int VISITOR_LIMIT_PER_MINUTE = 180;

    /**
     * 校验当前请求是否允许被统计
     */
    public boolean isAllowed(String ip, String visitorId) {
        String realIp = StringUtils.isBlank(ip) ? "unknown" : ip;
        String realVisitor = StringUtils.isBlank(visitorId) ? realIp : visitorId;

        return checkLimit(IP_LIMIT_PREFIX + realIp, IP_LIMIT_PER_MINUTE)
                && checkLimit(VISITOR_LIMIT_PREFIX + realVisitor, VISITOR_LIMIT_PER_MINUTE);
    }

    private boolean checkLimit(String key, int limit) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(1));
        }
        return count <= limit;
    }
}
