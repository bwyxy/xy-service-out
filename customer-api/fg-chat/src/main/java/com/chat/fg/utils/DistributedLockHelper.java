package com.chat.fg.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁帮助类
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockHelper {

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取锁并执行业务逻辑
     *
     * @param lockKey 锁Key
     * @param waitSeconds 最大等待时间（秒）
     * @param leaseSeconds 锁持有时间（秒）
     * @param supplier 业务逻辑
     * @param onConflict 获取锁失败时的提示
     * @return 业务结果
     */
    public <T> T tryLock(String lockKey, long waitSeconds, long leaseSeconds,
                         Supplier<T> supplier, String onConflict) {
        boolean locked = false;
        long deadline = System.currentTimeMillis() + waitSeconds * 1000;
        try {
            while (System.currentTimeMillis() < deadline) {
                locked = Boolean.TRUE.equals(
                        redisTemplate.opsForValue().setIfAbsent(lockKey, "1", leaseSeconds, TimeUnit.SECONDS)
                );
                if (locked) {
                    break;
                }
                Thread.sleep(100);
            }
            if (!locked) {
                throw new com.xy.common.exception.ConcurrentOperationException(onConflict);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new com.xy.common.exception.ConcurrentOperationException("获取锁被中断");
        } finally {
            if (locked) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception e) {
                    log.warn("释放分布式锁失败, key={}", lockKey, e);
                }
            }
        }
    }

    /**
     * 无返回值版本
     * */
    public void tryLock(String lockKey, long waitSeconds, long leaseSeconds,
                        Runnable runnable, String onConflict) {
        tryLock(lockKey, waitSeconds, leaseSeconds, () -> {
            runnable.run();
            return null;
        }, onConflict);
    }
}
