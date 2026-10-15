package com.xy.system.dispatch.support;

import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.system.dispatch.DispatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 分发平台限流器
 * 基于 Redis 做按平台、按自然分钟的固定窗口限流；Redis 不可用时降级放行并记录告警
 * */
@Slf4j
@Component
public class PlatformRateLimiter {

    private static final String KEY_PREFIX = "dispatch:rate:";

    private final StringRedisTemplate redisTemplate;

    public PlatformRateLimiter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 申请一次调用额度，超限时抛出 RATE_LIMITED 异常
     * @param platformCode 平台编码
     * @param limitPerMinute 每分钟允许的请求数，非正数表示不限流
     * */
    public void acquire(String platformCode, Integer limitPerMinute) {
        if (limitPerMinute == null || limitPerMinute <= 0) {
            return;
        }
        try {
            String key = buildKey(platformCode);
            Long current = redisTemplate.opsForValue().increment(key);
            if (current != null && current == 1L) {
                redisTemplate.expire(key, 70, TimeUnit.SECONDS);
            }
            if (current != null && current > limitPerMinute) {
                throw new DispatchException(DispatchErrorTypeEnum.RATE_LIMITED,
                        "平台【" + platformCode + "】本分钟调用次数已达上限 " + limitPerMinute + "，请稍后重试");
            }
        } catch (DispatchException e) {
            throw e;
        } catch (Exception e) {
            log.warn("分发限流校验降级放行 platform={} err={}", platformCode, e.getMessage());
        }
    }

    private String buildKey(String platformCode) {
        return KEY_PREFIX + platformCode + ":" + (System.currentTimeMillis() / 60000L);
    }
}
