package com.xy.system.service.impl;

import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.core.redis.RedisCache;
import com.xy.common.entity.SysUser;
import com.xy.common.entity.SysUserOnline;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.web.service.TokenBlacklistService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 在线用户服务单元测试
 * */
@ExtendWith(MockitoExtension.class)
class SysUserOnlineServiceImplTest {

    @Mock
    private HttpServletRequest request;

    private TestRedisCache redisCache;
    private TestJwtTokenProvider jwtTokenProvider;
    private TestTokenBlacklistService tokenBlacklistService;
    private SysUserOnlineServiceImpl userOnlineService;

    private LoginUser loginUser;

    @BeforeEach
    void setUp() {
        redisCache = new TestRedisCache();
        jwtTokenProvider = new TestJwtTokenProvider();
        tokenBlacklistService = new TestTokenBlacklistService();
        userOnlineService = new SysUserOnlineServiceImpl(redisCache, jwtTokenProvider, tokenBlacklistService);

        SysUser sysUser = new SysUser();
        sysUser.setId(1L);
        sysUser.setUsername("admin");
        sysUser.setStatus("0");
        sysUser.setRoles(Set.of("admin"));

        loginUser = new LoginUser(sysUser);
    }

    @Test
    void saveOnlineUser_shouldSuccess() {
        String token = "test_token";
        when(request.getHeader("x-forwarded-for")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("WL-Proxy-Client-IP")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");

        userOnlineService.saveOnlineUser(loginUser, token, request);

        assertTrue(redisCache.cache.containsKey("online:token:" + token));
        assertTrue(redisCache.set.contains(token));
        SysUserOnline online = (SysUserOnline) redisCache.cache.get("online:token:" + token);
        assertEquals("admin", online.getUsername());
        assertEquals("127.0.0.1", online.getIpaddr());
        assertEquals(token, loginUser.getToken());
    }

    @Test
    void saveOnlineUser_shouldReturn_whenTokenEmpty() {
        userOnlineService.saveOnlineUser(loginUser, "", request);

        assertTrue(redisCache.cache.isEmpty());
        assertTrue(redisCache.set.isEmpty());
    }

    @Test
    void saveOnlineUser_shouldReturn_whenSysUserNull() {
        LoginUser emptyUser = new LoginUser(null);

        userOnlineService.saveOnlineUser(emptyUser, "token", request);

        assertTrue(redisCache.cache.isEmpty());
        assertTrue(redisCache.set.isEmpty());
    }

    @Test
    void listOnlineUsers_shouldReturnFilteredList() {
        SysUserOnline online1 = new SysUserOnline();
        online1.setTokenId("token1");
        online1.setUsername("admin");
        online1.setIpaddr("192.168.1.1");
        online1.setLoginTime(1000L);

        SysUserOnline online2 = new SysUserOnline();
        online2.setTokenId("token2");
        online2.setUsername("test");
        online2.setIpaddr("192.168.1.2");
        online2.setLoginTime(2000L);

        redisCache.cache.put("online:token:token1", online1);
        redisCache.cache.put("online:token:token2", online2);
        redisCache.set.addAll(Arrays.asList("token1", "token2"));

        var list = userOnlineService.listOnlineUsers("192.168.1", "admin");

        assertEquals(1, list.size());
        assertEquals("admin", list.get(0).getUsername());
    }

    @Test
    void listOnlineUsers_shouldReturnEmpty_whenNoTokens() {
        var list = userOnlineService.listOnlineUsers(null, null);

        assertTrue(list.isEmpty());
    }

    @Test
    void listOnlineUsers_shouldCleanupExpiredToken() {
        redisCache.set.add("expired_token");

        var list = userOnlineService.listOnlineUsers(null, null);

        assertTrue(list.isEmpty());
        assertFalse(redisCache.set.contains("expired_token"));
    }

    @Test
    void forceLogout_shouldBlacklistAndDelete() {
        String tokenId = "token1";
        redisCache.cache.put("online:token:" + tokenId, new SysUserOnline());
        redisCache.set.add(tokenId);

        userOnlineService.forceLogout(tokenId);

        assertTrue(tokenBlacklistService.blacklistedTokens.contains(tokenId));
        assertFalse(redisCache.cache.containsKey("online:token:" + tokenId));
        assertFalse(redisCache.set.contains(tokenId));
    }

    @Test
    void removeOnlineUser_shouldDelete_whenTokenNotEmpty() {
        redisCache.cache.put("online:token:token1", new SysUserOnline());
        redisCache.set.add("token1");

        userOnlineService.removeOnlineUser("token1");

        assertFalse(redisCache.cache.containsKey("online:token:token1"));
        assertFalse(redisCache.set.contains("token1"));
    }

    @Test
    void removeOnlineUser_shouldNotDelete_whenTokenEmpty() {
        redisCache.cache.put("online:token:token1", new SysUserOnline());
        redisCache.set.add("token1");

        userOnlineService.removeOnlineUser("");

        assertTrue(redisCache.cache.containsKey("online:token:token1"));
        assertTrue(redisCache.set.contains("token1"));
    }

    static class TestRedisCache extends RedisCache {
        private final Map<String, Object> cache = new HashMap<>();
        private final Set<String> set = new HashSet<>();

        @Override
        public <T> void setCacheObject(String key, T value, Integer timeout, TimeUnit timeUnit) {
            cache.put(key, value);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getCacheObject(String key) {
            return (T) cache.get(key);
        }

        @Override
        public boolean deleteObject(String key) {
            return cache.remove(key) != null;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> Set<T> getCacheSet(String key) {
            return (Set<T>) new HashSet<>(set);
        }

        @Override
        public <T> Long addCacheSetMember(String key, T value) {
            set.add((String) value);
            return 1L;
        }

        @Override
        public <T> Long removeCacheSetMember(String key, T value) {
            return set.remove(value) ? 1L : 0L;
        }
    }

    static class TestJwtTokenProvider extends JwtTokenProvider {
        @Override
        public long getRemainingExpiration(String token) {
            return 3600000L;
        }
    }

    static class TestTokenBlacklistService extends TokenBlacklistService {
        private final Set<String> blacklistedTokens = new HashSet<>();

        @Override
        public void blacklist(String token, long expirationMillis) {
            blacklistedTokens.add(token);
        }
    }
}
