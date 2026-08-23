package com.chat.ai.security;

import com.chat.ai.config.AiServiceProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 内部 API Key 认证过滤器
 * 允许后台管理服务通过 X-Internal-Api-Key 调用 xy-ai 接口
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final AiServiceProperties aiServiceProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String internalKey = request.getHeader(INTERNAL_API_KEY_HEADER);
        String configKey = aiServiceProperties.getInternalApiKey();

        log.info("InternalApiKeyFilter 收到请求: uri={}, headerKey={}, configKeyEmpty={}",
                request.getRequestURI(),
                internalKey,
                !StringUtils.hasText(configKey));

        if (!StringUtils.hasText(configKey)) {
            log.error("ai.service.internal-api-key 未配置，拒绝内部调用");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"ai.service.internal-api-key not configured\"}");
            return;
        }

        if (StringUtils.hasText(internalKey) && internalKey.equals(configKey)) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "internal-service",
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("内部服务调用认证成功: uri={}", request.getRequestURI());
        } else {
            log.warn("内部服务调用认证失败: uri={}, headerKeyPresent={}",
                    request.getRequestURI(),
                    StringUtils.hasText(internalKey));
        }

        filterChain.doFilter(request, response);
    }
}
