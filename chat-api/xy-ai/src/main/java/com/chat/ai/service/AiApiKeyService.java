package com.chat.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.ai.dto.TokenResponse;
import com.chat.ai.entity.AiApiKey;

/**
 * AI 模块 API 密钥服务
 */
public interface AiApiKeyService extends IService<AiApiKey> {

    /**
     * 通过 API Key 和 API Secret 换取访问令牌
     *
     * @param apiKey    访问密钥 Key
     * @param apiSecret 访问密钥 Secret
     * @return Token 响应
     */
    TokenResponse exchangeToken(String apiKey, String apiSecret);
}
