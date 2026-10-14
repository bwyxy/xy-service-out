package com.xy.framework.security.filter;

import com.alibaba.fastjson.JSON;
import com.xy.common.core.domain.R;
import com.xy.common.core.redis.RedisCache;
import com.xy.common.enums.ResultCode;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.web.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import com.xy.common.utils.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Jwt 认证过滤器
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private RedisCache redisCache;

    private static final String ONLINE_TOKEN_KEY = "online:token:";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt)) {

                // 会话已被同类型设备顶下线 / 已登出：立即返回指定失效码并终止链路。
                // 注意：此处必须直接 return，不能落入下方的 filterChain.doFilter，
                // 否则请求会继续传递到控制器，失效校验形同虚设。
                if (tokenBlacklistService.isBlacklisted(jwt)) {
                    log.warn("会话已失效（被顶下线或已登出），拒绝请求 uri={}", request.getRequestURI());
                    writeSessionExpired(response);
                    return;
                }

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
            // 解析/校验失败时不设置认证信息，由后续的 AuthenticationEntryPoint 统一返回 401
            log.error("无法设置用户认证信息：{}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 返回「登录态已失效」的指定错误码
     * */
    private void writeSessionExpired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(R.error(ResultCode.SESSION_EXPIRED)));
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        log.info("请求头: {}", jwtTokenProvider.getJwtHeader());
        String bearerToken = request.getHeader(jwtTokenProvider.getJwtHeader());
        String prefix = jwtTokenProvider.getJwtPrefix() + " ";

        if (StringUtils.isEmpty(prefix) && StringUtils.hasText(bearerToken)) {
            return bearerToken;
        }
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(prefix)) {
            return bearerToken.substring(prefix.length());
        }
        return null;
    }
}
