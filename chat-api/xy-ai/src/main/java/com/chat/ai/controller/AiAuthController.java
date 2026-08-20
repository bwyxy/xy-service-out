package com.chat.ai.controller;

import com.chat.ai.dto.TokenRequest;
import com.chat.ai.dto.TokenResponse;
import com.chat.ai.service.AiApiKeyService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 模块认证接口
 */
@Slf4j
@RestController
@RequestMapping("/ai/auth")
public class AiAuthController {

    @Autowired
    private AiApiKeyService apiKeyService;

    /**
     * 使用 API Key + Secret 换取访问令牌
     */
    @Anonymous
    @PostMapping("/token")
    public R<TokenResponse> token(@Valid @RequestBody TokenRequest request) {
        long start = System.currentTimeMillis();
        TokenResponse response = apiKeyService.exchangeToken(request.getApiKey(), request.getApiSecret());
        log.info("API Key 换 token 成功, apiKey={}, cost={}ms", request.getApiKey(), System.currentTimeMillis() - start);
        return R.ok(response);
    }
}
