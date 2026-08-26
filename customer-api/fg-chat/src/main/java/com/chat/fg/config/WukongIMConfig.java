package com.chat.fg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 悟空IM HTTP API 配置
 */
@Configuration
public class WukongIMConfig {

    @Bean
    @ConfigurationProperties(prefix = "wukongim")
    public WukongIMProperties wukongIMProperties(
            @Value("${xy.wk-api-url}") String wkApiUrl,
            @Value("${xy.wk-wss-url}") String wkWSSUrl
    ) {

        WukongIMProperties properties = new WukongIMProperties();
        properties.setApiUrl(wkApiUrl);
        properties.setWsUrl(wkWSSUrl);

        return new WukongIMProperties();
    }

    @Bean
    public OkHttpClient wkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Bean
    public ObjectMapper wukongObjectMapper() {
        return new ObjectMapper();
    }

    @Data
    public static class WukongIMProperties {
        /** API 地址 */
        private String apiUrl;
        /** WebSocket 地址 */
        private String wsUrl;
        /** 管理员密钥 */
        private String managerToken = "";
    }
}