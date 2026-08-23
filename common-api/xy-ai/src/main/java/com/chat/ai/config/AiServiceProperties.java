package com.chat.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务内部调用配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceProperties {

    /**
     * 内部调用 API Key，用于后台管理服务调用 xy-ai
     */
    private String internalApiKey;
}
