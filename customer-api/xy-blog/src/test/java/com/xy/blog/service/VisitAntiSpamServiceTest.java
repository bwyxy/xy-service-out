package com.xy.blog.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * 访问统计防刷服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class VisitAntiSpamServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private VisitAntiSpamService antiSpamService;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void isAllowed_shouldPassWhenUnderLimit() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 1L);

        assertTrue(antiSpamService.isAllowed("127.0.0.1", "visitor-1"));
    }

    @Test
    void isAllowed_shouldBlockWhenIpExceedsLimit() {
        when(valueOperations.increment(startsWith("blog:visit:limit:ip:"))).thenReturn(200L);
        lenient().when(valueOperations.increment(startsWith("blog:visit:limit:visitor:"))).thenReturn(1L);

        assertFalse(antiSpamService.isAllowed("127.0.0.1", "visitor-1"));
    }

    @Test
    void isAllowed_shouldBlockWhenVisitorExceedsLimit() {
        when(valueOperations.increment(startsWith("blog:visit:limit:ip:"))).thenReturn(1L);
        when(valueOperations.increment(startsWith("blog:visit:limit:visitor:"))).thenReturn(300L);

        assertFalse(antiSpamService.isAllowed("127.0.0.1", "visitor-1"));
    }

    @Test
    void isAllowed_shouldUseIpAsFallbackVisitor() {
        when(valueOperations.increment(anyString())).thenReturn(1L, 1L);

        assertTrue(antiSpamService.isAllowed("127.0.0.1", null));
    }
}
