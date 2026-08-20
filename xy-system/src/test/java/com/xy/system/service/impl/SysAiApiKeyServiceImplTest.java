package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.common.dto.ai.ApiKeyGenerateResponse;
import com.xy.common.dto.ai.TokenResponse;
import com.xy.common.entity.AiApiKey;
import com.xy.common.exception.ServiceException;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.system.mapper.SysAiApiKeyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AI API 密钥服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class SysAiApiKeyServiceImplTest {

    @Mock
    private SysAiApiKeyMapper apiKeyMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private SysAiApiKeyServiceImpl apiKeyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(apiKeyService, "baseMapper", apiKeyMapper);
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "test-jwt-secret-key-at-least-32-bytes-long");
        ReflectionTestUtils.setField(apiKeyService, "jwtTokenProvider", jwtTokenProvider);
    }

    @Test
    void exchangeToken_shouldReturnTokenWhenCredentialsValid() {
        AiApiKey entity = createKey(1L, "xyai_test", "$2a$10$encoded", 1, null, 3600);
        when(apiKeyMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(entity);
        when(passwordEncoder.matches("plain-secret", entity.getApiSecret())).thenReturn(true);

        TokenResponse response = apiKeyService.exchangeToken("xyai_test", "plain-secret");

        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(3600L, response.getExpiresIn());
    }

    @Test
    void exchangeToken_shouldThrowWhenKeyDisabled() {
        AiApiKey entity = createKey(1L, "xyai_test", "$2a$10$encoded", 0, null, 3600);
        when(apiKeyMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(entity);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> apiKeyService.exchangeToken("xyai_test", "plain-secret"));
        assertTrue(exception.getMessage().contains("禁用"));
    }

    @Test
    void exchangeToken_shouldThrowWhenSecretMismatch() {
        AiApiKey entity = createKey(1L, "xyai_test", "$2a$10$encoded", 1, null, 3600);
        when(apiKeyMapper.selectOne(any(LambdaQueryWrapper.class), anyBoolean())).thenReturn(entity);
        when(passwordEncoder.matches("wrong-secret", entity.getApiSecret())).thenReturn(false);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> apiKeyService.exchangeToken("xyai_test", "wrong-secret"));
        assertTrue(exception.getMessage().contains("不正确"));
    }

    @Test
    void generate_shouldReturnPlainSecretAndSaveEncodedSecret() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-secret");
        when(apiKeyMapper.insert(any(AiApiKey.class))).thenAnswer(invocation -> {
            AiApiKey arg = invocation.getArgument(0);
            arg.setId(100L);
            return 1;
        });

        ApiKeyGenerateResponse response = apiKeyService.generate("测试应用", 7200);

        assertNotNull(response);
        assertEquals("测试应用", response.getAppName());
        assertEquals(7200, response.getTokenExpireSeconds());
        assertNotNull(response.getApiKey());
        assertTrue(response.getApiKey().startsWith("xyai_"));
        assertNotNull(response.getApiSecret());
        assertNotEquals("encoded-secret", response.getApiSecret());
        verify(apiKeyMapper, times(1)).insert(any(AiApiKey.class));
    }

    @Test
    void generate_shouldUseDefaultExpireWhenInvalid() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-secret");
        when(apiKeyMapper.insert(any(AiApiKey.class))).thenAnswer(invocation -> {
            AiApiKey arg = invocation.getArgument(0);
            arg.setId(101L);
            return 1;
        });

        ApiKeyGenerateResponse response = apiKeyService.generate("测试应用", -1);

        assertEquals(3600, response.getTokenExpireSeconds());
    }

    @Test
    void enable_shouldSetEnabledToOne() {
        AiApiKey entity = createKey(1L, "xyai_test", "secret", 0, null, 3600);
        when(apiKeyMapper.selectById(1L)).thenReturn(entity);
        when(apiKeyMapper.updateById(any(AiApiKey.class))).thenReturn(1);

        apiKeyService.enable(1L);

        assertEquals(1, entity.getEnabled());
        verify(apiKeyMapper, times(1)).updateById(entity);
    }

    @Test
    void disable_shouldSetEnabledToZero() {
        AiApiKey entity = createKey(1L, "xyai_test", "secret", 1, null, 3600);
        when(apiKeyMapper.selectById(1L)).thenReturn(entity);
        when(apiKeyMapper.updateById(any(AiApiKey.class))).thenReturn(1);

        apiKeyService.disable(1L);

        assertEquals(0, entity.getEnabled());
        verify(apiKeyMapper, times(1)).updateById(entity);
    }

    @Test
    void delete_shouldRemoveById() {
        when(apiKeyMapper.deleteById(1L)).thenReturn(1);

        apiKeyService.delete(1L);

        verify(apiKeyMapper, times(1)).deleteById(1L);
    }

    @Test
    void delete_shouldThrowWhenRemoveFailed() {
        when(apiKeyMapper.deleteById(1L)).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> apiKeyService.delete(1L));
        assertTrue(exception.getMessage().contains("删除失败"));
    }

    private AiApiKey createKey(Long id, String apiKey, String apiSecret, Integer enabled,
                               java.time.LocalDateTime expireTime, Integer tokenExpireSeconds) {
        AiApiKey entity = new AiApiKey();
        entity.setId(id);
        entity.setApiKey(apiKey);
        entity.setApiSecret(apiSecret);
        entity.setEnabled(enabled);
        entity.setExpireTime(expireTime);
        entity.setTokenExpireSeconds(tokenExpireSeconds);
        return entity;
    }
}
