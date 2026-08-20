package com.chat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.ai.dto.TokenResponse;
import com.chat.ai.entity.AiApiKey;
import com.chat.ai.exception.AiException;
import com.chat.ai.mapper.AiApiKeyMapper;
import com.chat.ai.service.AiApiKeyService;
import com.xy.common.provider.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 模块 API 密钥服务实现
 */
@Service
public class AiApiKeyServiceImpl extends ServiceImpl<AiApiKeyMapper, AiApiKey>
        implements AiApiKeyService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public TokenResponse exchangeToken(String apiKey, String apiSecret) {
        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            throw new AiException("apiKey 和 apiSecret 不能为空");
        }

        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getApiKey, apiKey)
                .last("limit 1");
        AiApiKey key = getOne(wrapper);

        if (key == null) {
            throw new AiException("API Key 不存在");
        }
        if (key.getEnabled() == null || key.getEnabled() != 1) {
            throw new AiException("API Key 已被禁用");
        }
        if (key.getExpireTime() != null && key.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new AiException("API Key 已过期");
        }
        if (!passwordEncoder.matches(apiSecret, key.getApiSecret())) {
            // 为了兼容明文存储的密钥，也支持明文对比（生产环境建议全部使用 BCrypt）
            if (!apiSecret.equals(key.getApiSecret())) {
                throw new AiException("API Secret 不正确");
            }
        }

        long expireSeconds = key.getTokenExpireSeconds() != null
                ? key.getTokenExpireSeconds()
                : 3600L;
        long expireMillis = expireSeconds * 1000L;

        // subject 使用 apiKey，便于后续追踪调用来源
        String accessToken = jwtTokenProvider.createToken(key.getApiKey(), expireMillis);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expireSeconds)
                .build();
    }
}
