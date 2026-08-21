package com.chat.ai.security;

import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.utils.StringUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * AI 模块 JWT 认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtTokenProvider.validateToken(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("设置安全上下文成功，用户：{}", username);
                }
            }
        } catch (Exception e) {
            log.error("无法设置用户认证信息：{}", e.getMessage());
            throw new SecurityException("获取登录失败");
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String headerName = jwtTokenProvider.getJwtHeader();
        String bearerToken = request.getHeader(headerName);
        String prefix = jwtTokenProvider.getJwtPrefix();

        log.warn("headerName-{},token-{},bearerToken-{}", headerName, prefix, bearerToken);
        if (StringUtils.isEmpty(prefix) && StringUtils.hasText(bearerToken)) {
            return bearerToken;
        }
        if (StringUtils.hasText(bearerToken) && prefix != null && bearerToken.startsWith(prefix + " ")) {
            return bearerToken.substring(prefix.length() + 1);
        }
        return null;
    }
}
