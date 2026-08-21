package com.chat.fg.config;

import com.chat.fg.client.AiAuthClient;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.TokenRequest;
import com.xy.common.dto.ai.TokenResponse;
import com.xy.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * xy-ai API Key Token 提供者
 *
 * <p>负责用 API Key + Secret 换取 token 并在内存中缓存，避免每次请求都调用 /ai/auth/token。</p>
 */
@Slf4j
@Component
public class AiApiKeyTokenProvider {

    @Value("${xy-ai.apiKey:}")
    private String apiKey;

    @Value("${xy-ai.secret:}")
    private String apiSecret;

    private final AiAuthClient aiAuthClient;

    private volatile String accessToken;
    private volatile long expireAt;

    /**
     * 提前刷新阈值（毫秒），在 token 过期前 60 秒刷新
     */
    private static final long REFRESH_THRESHOLD_MS = 60_000L;

    public AiApiKeyTokenProvider(AiAuthClient aiAuthClient) {
        this.aiAuthClient = aiAuthClient;
    }

    /**
     * 获取有效的访问令牌
     */
    public String getToken() {
        if (!isConfigured()) {
            return null;
        }
        if (accessToken == null || System.currentTimeMillis() >= expireAt - REFRESH_THRESHOLD_MS) {
            synchronized (this) {
                if (accessToken == null || System.currentTimeMillis() >= expireAt - REFRESH_THRESHOLD_MS) {
                    refreshToken();
                }
            }
        }
        return accessToken;
    }

    private void refreshToken() {
        log.info("正在使用 API Key 刷新 xy-ai access token");
        R<TokenResponse> response = aiAuthClient.getToken(new TokenRequest(apiKey, apiSecret));
        if (response == null || response.getData() == null) {
            throw new BusinessException("获取 xy-ai token 失败：响应为空");
        }
        TokenResponse data = response.getData();
        this.accessToken = data.getAccessToken();
        this.expireAt = System.currentTimeMillis() + data.getExpiresIn() * 1000L;
        log.info("xy-ai access token 刷新成功，有效期 {} 秒", data.getExpiresIn());
    }

    private boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && apiSecret != null && !apiSecret.isEmpty();
    }
}
