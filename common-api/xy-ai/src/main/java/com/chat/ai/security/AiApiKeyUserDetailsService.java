package com.chat.ai.security;

import com.xy.common.entity.AiApiKey;
import com.xy.system.service.ISysAiApiKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * AI API Key 用户详情服务
 *
 * <p>仅处理以 {@code xyai_} 开头的 API Key 调用方，用于 JWT token 认证。</p>
 */
@Service
@RequiredArgsConstructor
public class AiApiKeyUserDetailsService implements UserDetailsService {

    private static final String API_KEY_PREFIX = "xyai_";

    private final ISysAiApiKeyService aiApiKeyService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || !username.startsWith(API_KEY_PREFIX)) {
            throw new UsernameNotFoundException("不支持的认证主体: " + username);
        }
        AiApiKey apiKey = aiApiKeyService.getByApiKey(username);
        if (apiKey == null || apiKey.getEnabled() == null || apiKey.getEnabled() != 1) {
            throw new UsernameNotFoundException(username + " API Key 不存在或已禁用");
        }
        return User.builder()
                .username(apiKey.getApiKey())
                .password(apiKey.getApiSecret())
                .authorities(Collections.singleton(new SimpleGrantedAuthority("ai:chat")))
                .build();
    }
}
