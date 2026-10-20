package com.chat.fg.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * 跨域配置
 *
 * 桌面端（Tauri）页面自身的源是 tauri://localhost（Windows 为 http://tauri.localhost），
 * 访问后端接口必然跨域，需要显式放行；Web 端与后端同源，不受影响。
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的来源：桌面端（macOS/Linux 为 tauri://localhost，Windows 为 http://tauri.localhost）+ 线上站点
     */
    private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
            "tauri://localhost",
            "http://tauri.localhost",
            "https://www.bwyxy.cn"
    );

    /**
     * 以 Filter 形式注册跨域处理，并置于最高优先级
     *
     * 必须早于 Spring Security 过滤器链（默认 order = -100）执行：
     * 浏览器预检 OPTIONS 请求不携带 token，若先经过认证过滤器会被判为未登录，
     * 预检返回 401 则跨域请求直接失败。
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
