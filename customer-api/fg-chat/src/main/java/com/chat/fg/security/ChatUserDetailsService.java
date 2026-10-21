package com.chat.fg.security;

import com.chat.fg.service.IUserService;
import com.xy.common.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 复合 UserDetailsService
 * 
 * - username 以 "chat_" 开头 → 从 t_user 表加载客户端用户
 * - 其他 → 委托给原有的 UserDetailsServiceImpl（从 sys_user 表加载管理后台用户）
 */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class ChatUserDetailsService implements UserDetailsService {

    private final IUserService userService;
    private final UserDetailsService originalUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("用户名不能为空");
        }

        // 聊天客户端用户：username 格式为 chat_{uId}
        if (username.startsWith("chat_")) {
            try {
                Long uId = Long.parseLong(username.substring(5));
                User user = userService.getUserInfo(uId);
                if (user == null) {
                    throw new UsernameNotFoundException("聊天用户不存在: " + username);
                }
                return new ChatUserDetails(user);
            } catch (NumberFormatException e) {
                throw new UsernameNotFoundException("聊天用户ID格式错误: " + username);
            }
        }

        // 管理后台用户：委托给原有实现
        return originalUserDetailsService.loadUserByUsername(username);
    }
}