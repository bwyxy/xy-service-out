package com.xy.common.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 全局配置：自动透传当前请求的 token 请求头
 */
@Slf4j
@Configuration
public class FeignConfig {

    private static final String TOKEN_HEADER = "token";

    @Bean
    public RequestInterceptor tokenRequestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String token = request.getHeader(TOKEN_HEADER);
            if (token == null || token.isEmpty()) {
                token = request.getParameter(TOKEN_HEADER);
            }
            if (token != null && !token.isEmpty()) {
                requestTemplate.header(TOKEN_HEADER, token);
                log.debug("Feign 请求透传 token 成功");
            }
        };
    }
}
