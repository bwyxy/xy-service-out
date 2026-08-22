package com.xy.system.config.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 服务调用配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ai.service")
public class AiServiceClientConfig {

    /**
     * xy-ai 服务地址
     */
    private String url = "http://localhost:9070";

    /**
     * 内部调用 API Key，用于后台管理服务调用 xy-ai
     */
    private String internalApiKey = "";
}
