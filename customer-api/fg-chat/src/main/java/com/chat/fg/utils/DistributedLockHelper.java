package com.chat.fg.utils;

import com.xy.common.exception.ConcurrentOperationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁帮助类
 *
 * <p>锁 value 为唯一的 UUID，释放时用 Lua 脚本做「比对后再删除」，
 * 避免业务执行超过租约时间、锁已被他人重新获取时，本线程仍把别人的锁删掉。</p>
 *
 * <p>注意：调用方需保证业务耗时小于 {@code leaseSeconds}，否则互斥性无法保证
 * （超时后锁自动过期，其它线程可进入临界区）。</p>
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedLockHelper {

    /** 仅当锁 value 仍属于当前线程时才删除，否则说明锁已过期并被他人持有 */
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    /** 抢锁失败后的重试间隔 */
    private static final long RETRY_INTERVAL_MILLIS = 100L;

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取锁并执行业务逻辑
     *
     * @param lockKey 锁Key
     * @param waitSeconds 最大等待时间（秒）
     * @param leaseSeconds 锁持有时间（秒），需大于业务执行耗时
     * @param supplier 业务逻辑
     * @param onConflict 获取锁失败时的提示
     * @return 业务结果
     */
    public <T> T tryLock(String lockKey, long waitSeconds, long leaseSeconds,
                         Supplier<T> supplier, String onConflict) {
        String lockValue = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + waitSeconds * 1000;
        boolean locked = false;

        try {
            while (System.currentTimeMillis() < deadline) {
                locked = Boolean.TRUE.equals(
                        redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, leaseSeconds, TimeUnit.SECONDS)
                );
                if (locked) {
                    break;
                }
                Thread.sleep(RETRY_INTERVAL_MILLIS);
            }
            if (!locked) {
                throw new ConcurrentOperationException(onConflict);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConcurrentOperationException("获取锁被中断");
        } finally {
            if (locked) {
                release(lockKey, lockValue);
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

    /**
     * 释放锁：比对 value 后再删除，防止误删他人持有的锁
     * */
    private void release(String lockKey, String lockValue) {
        try {
            redisTemplate.execute(RELEASE_SCRIPT, Collections.singletonList(lockKey), lockValue);
        } catch (Exception e) {
            log.warn("释放分布式锁失败, key={}", lockKey, e);
        }
    }
}
