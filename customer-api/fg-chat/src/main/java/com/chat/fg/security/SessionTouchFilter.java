package com.chat.fg.security;

import com.chat.fg.service.ILoginSessionService;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.utils.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 会话活跃度刷新过滤器
 *
 * <p>携带有效会话 token 的请求会刷新会话的最后活跃时间（内部节流，
 * 默认 5 分钟最多写一次 Redis），保证会话在线状态可追溯。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionTouchFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final ILoginSessionService loginSessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (StringUtils.isNotEmpty(token)) {
            try {
                loginSessionService.touch(token);
            } catch (Exception e) {
                // 活跃度刷新失败不影响正常业务请求
                log.warn("刷新会话活跃时间失败: {}", e.getMessage());
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(jwtTokenProvider.getJwtHeader());
        if (StringUtils.isEmpty(bearerToken)) {
            return null;
        }
        String prefix = jwtTokenProvider.getJwtPrefix() + " ";
        if (bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length());
        }
        return bearerToken;
    }
}