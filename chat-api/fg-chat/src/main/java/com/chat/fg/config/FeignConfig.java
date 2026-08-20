package com.chat.fg.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 全局配置
 * <p>
 * 自动将当前请求的登录 token 透传给下游服务。
 * </p>
 */
@Configuration
public class FeignConfig {

    private static final String TOKEN_HEADER = "token";

    @Bean
    public RequestInterceptor tokenRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                ServletRequestAttributes attributes = (ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes();
                if (attributes == null) {
                    return;
                }
                HttpServletRequest request = attributes.getRequest();
                String token = request.getHeader(TOKEN_HEADER);
                if (token != null && !token.isEmpty()) {
                    template.header(TOKEN_HEADER, token);
                }
            }
        };
    }
}
