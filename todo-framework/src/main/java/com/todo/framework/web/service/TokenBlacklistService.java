package com.todo.framework.web.service;

import com.todo.common.core.redis.RedisCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class TokenBlacklistService {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisCache redisCache;

    public void blacklist(String token, long expirationMillis) {

        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "logout",
                Duration.ofMillis(expirationMillis)
        );
    }

    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}
