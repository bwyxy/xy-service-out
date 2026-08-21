package com.xy.framework.web.service;

import com.xy.common.entity.AiApiKey;
import com.xy.system.service.ISysAiApiKeyService;
import com.xy.system.service.ISysUserService;
import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.entity.SysUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String API_KEY_PREFIX = "xyai_";

    private final ISysUserService sysUserService;

    private final ISysAiApiKeyService aiApiKeyService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // API Key 调用方走独立认证
        if (username != null && username.startsWith(API_KEY_PREFIX)) {
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

        SysUser sysUser = sysUserService.getByUsername(username);

        if (sysUser == null) {
            throw new UsernameNotFoundException(username + " 用户不存在");
        }

        return new LoginUser(sysUser);
    };
}
