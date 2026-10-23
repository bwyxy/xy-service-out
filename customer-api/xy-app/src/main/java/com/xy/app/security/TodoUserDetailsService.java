package com.xy.app.security;

import com.xy.common.entity.User;
import com.xy.system.service.IChatUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 复合 UserDetailsService
 * */
@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class TodoUserDetailsService implements UserDetailsService {

    private final IChatUserService chatUserService;

    private final UserDetailsService originalUserDetailsService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isEmpty()) {
            throw new UsernameNotFoundException("用户名不能为空");
        }

        // 待办客户端用户：username 格式为 todo_{uId}
        if (username.startsWith(TodoUserDetails.USERNAME_PREFIX)) {
            String rawUId = username.substring(TodoUserDetails.USERNAME_PREFIX.length());

            Long uId;
            try {
                uId = Long.parseLong(rawUId);
            } catch (NumberFormatException e) {
                throw new UsernameNotFoundException("待办用户ID格式错误: " + username);
            }

            User user = chatUserService.getByUId(uId);
            if (user == null) {
                throw new UsernameNotFoundException("待办用户不存在: " + username);
            }

            return new TodoUserDetails(user);
        }

        // 后台管理端用户：委托给原有实现
        return originalUserDetailsService.loadUserByUsername(username);
    }
}
