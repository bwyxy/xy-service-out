package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSON;
import com.chat.fg.enums.SessionStatus;
import com.chat.fg.pojo.LoginSession;
import com.chat.fg.service.ILoginSessionService;
import com.chat.fg.utils.DistributedLockHelper;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.web.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 登录会话服务（Redis 实现）
 *
 * <p>存储结构：</p>
 * <ul>
 *   <li>会话明细：{@code login:session:{token}} → LoginSession JSON，TTL 与 token 剩余有效期对齐</li>
 *   <li>用户全量会话索引：{@code login:user:{uid}} → SET&lt;token&gt;</li>
 *   <li>设备类型会话索引：{@code login:user:{uid}:type:{deviceType}} → SET&lt;token&gt;</li>
 *   <li>互斥锁：{@code login:lock:{uid}:{deviceType}}</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginSessionServiceImpl implements ILoginSessionService {

    private static final String SESSION_KEY_PREFIX = "login:session:";
    private static final String USER_KEY_PREFIX = "login:user:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";

    /** 最后活跃时间刷新的节流间隔，避免每个请求都写 Redis */
    private static final long TOUCH_THROTTLE_MS = 5 * 60 * 1000L;

    /** 互斥锁持有时间（秒） */
    private static final long LOCK_LEASE_SECONDS = 10L;

    private final StringRedisTemplate redisTemplate;
    private final DistributedLockHelper lockHelper;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public List<LoginSession> replaceSession(LoginSession session, long ttlMillis) {
        Long uid = session.getUid();
        Integer deviceType = session.getDeviceType();

        String lockKey = LOCK_KEY_PREFIX + uid + ":" + deviceType;
        return lockHelper.tryLock(
                lockKey,
                2,
                LOCK_LEASE_SECONDS,
                () -> {
                    // 1. 同设备类型下的旧会话全部标记失效
                    List<LoginSession> kicked = kickSameTypeSessions(uid, deviceType);

                    // 2. 写入新会话并建立索引
                    writeSession(session, ttlMillis);
                    indexSession(session, ttlMillis);

                    return kicked;
                },
                "登录处理中，请稍后重试"
        );
    }

    @Override
    public LoginSession getSession(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        String json = redisTemplate.opsForValue().get(sessionKey(token));
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return JSON.parseObject(json, LoginSession.class);
        } catch (Exception e) {
            log.warn("解析登录会话失败, token={}", token, e);
            return null;
        }
    }

    @Override
    public List<LoginSession> listActiveSessions(Long uid) {
        return collectActiveSessions(USER_KEY_PREFIX + uid);
    }

    @Override
    public List<LoginSession> listActiveSessions(Long uid, Integer deviceType) {
        return collectActiveSessions(typeKey(uid, deviceType));
    }

    @Override
    public void touch(String token) {
        LoginSession session = getSession(token);
        if (session == null || !SessionStatus.ACTIVE.getCode().equals(session.getStatus())) {
            return;
        }

        long now = System.currentTimeMillis();
        Long lastActiveTime = session.getLastActiveTime();
        if (lastActiveTime != null && now - lastActiveTime < TOUCH_THROTTLE_MS) {
            return;
        }

        long remaining = remainingTtl(token);
        if (remaining <= 0) {
            return;
        }

        session.setLastActiveTime(now);
        // 仅刷新会话明细，不再写入索引（避免把已失效会话重新加回活跃集合）
        writeSession(session, remaining);
    }

    @Override
    public void endSession(String token, SessionStatus status) {
        if (token == null || token.isEmpty()) {
            return;
        }

        LoginSession session = getSession(token);
        long remaining = remainingTtl(token);

        if (session == null) {
            // 明细已随 TTL 过期，仅需拉黑 token
            blacklist(token, remaining);
            return;
        }

        session.setStatus(status.getCode());

        if (remaining > 0) {
            // 保留失效记录（status=KICKED/LOGOUT）便于追溯，TTL 与 token 剩余有效期对齐
            writeSession(session, remaining);
            blacklist(token, remaining);
        } else {
            redisTemplate.delete(sessionKey(token));
        }

        removeFromIndex(session);
    }

    // ==================== 内部方法 ====================

    /**
     * 踢掉指定用户 + 设备类型下的全部活跃会话
     */
    private List<LoginSession> kickSameTypeSessions(Long uid, Integer deviceType) {
        String typeKey = typeKey(uid, deviceType);
        Set<String> tokens = redisTemplate.opsForSet().members(typeKey);
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<LoginSession> kicked = new ArrayList<>();
        for (String token : tokens) {
            LoginSession session = getSession(token);
            if (session == null) {
                // 明细已过期，清理索引残留
                redisTemplate.opsForSet().remove(typeKey, token);
                redisTemplate.opsForSet().remove(USER_KEY_PREFIX + uid, token);
                continue;
            }
            if (SessionStatus.ACTIVE.getCode().equals(session.getStatus())) {
                kicked.add(session);
            }
            endSession(token, SessionStatus.KICKED);
        }
        return kicked;
    }

    /**
     * 写入会话明细
     */
    private void writeSession(LoginSession session, long ttlMillis) {
        String key = sessionKey(session.getToken());
        if (ttlMillis <= 0) {
            redisTemplate.delete(key);
            return;
        }
        redisTemplate.opsForValue().set(
                key,
                JSON.toJSONString(session),
                ttlMillis,
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * 建立会话索引（用户维度 + 设备类型维度）
     */
    private void indexSession(LoginSession session, long ttlMillis) {
        if (ttlMillis <= 0) {
            return;
        }
        String token = session.getToken();
        String typeKey = typeKey(session.getUid(), session.getDeviceType());

        redisTemplate.opsForSet().add(typeKey, token);
        redisTemplate.expire(typeKey, ttlMillis, TimeUnit.MILLISECONDS);

        String userKey = USER_KEY_PREFIX + session.getUid();
        redisTemplate.opsForSet().add(userKey, token);
        redisTemplate.expire(userKey, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * 从会话索引中移除
     */
    private void removeFromIndex(LoginSession session) {
        String token = session.getToken();
        redisTemplate.opsForSet().remove(typeKey(session.getUid(), session.getDeviceType()), token);
        redisTemplate.opsForSet().remove(USER_KEY_PREFIX + session.getUid(), token);
    }

    /**
     * 依据索引集合收集活跃会话
     */
    private List<LoginSession> collectActiveSessions(String indexKey) {
        Set<String> tokens = redisTemplate.opsForSet().members(indexKey);
        if (tokens == null || tokens.isEmpty()) {
            return Collections.emptyList();
        }

        List<LoginSession> sessions = new ArrayList<>();
        for (String token : tokens) {
            LoginSession session = getSession(token);
            if (session == null) {
                redisTemplate.opsForSet().remove(indexKey, token);
                continue;
            }
            if (SessionStatus.ACTIVE.getCode().equals(session.getStatus())) {
                sessions.add(session);
            }
        }
        return sessions;
    }

    private void blacklist(String token, long remainingMillis) {
        if (remainingMillis <= 0) {
            return;
        }
        try {
            tokenBlacklistService.blacklist(token, remainingMillis);
        } catch (Exception e) {
            log.warn("拉黑会话 token 失败, token={}", token, e);
        }
    }

    private long remainingTtl(String token) {
        try {
            return jwtTokenProvider.getRemainingExpiration(token);
        } catch (Exception e) {
            log.warn("解析 token 剩余有效期失败, token={}", token, e);
            return 0;
        }
    }

    private String sessionKey(String token) {
        return SESSION_KEY_PREFIX + token;
    }

    private String typeKey(Long uid, Integer deviceType) {
        return USER_KEY_PREFIX + uid + ":type:" + deviceType;
    }
}