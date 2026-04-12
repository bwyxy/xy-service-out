package com.todo.admin.service.impl;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public CustomUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!StringUtils.hasText(username)) {
            throw new UsernameNotFoundException("用户名不能为空");
        }

        String sql = "select username, password, status from sys_user where username = ? and deleted = 0";
        try {
            Map<String, Object> row = jdbcTemplate.queryForMap(sql, username);
            String dbUsername = (String) row.get("username");
            String password = (String) row.get("password");
            // 简化：如果是 admin，授予 ADMIN 角色，否则授予 USER
            String role = "ROLE_USER";
            if ("admin".equals(dbUsername)) {
                role = "ROLE_ADMIN";
            }

            return User.withUsername(dbUsername)
                    .password(password)
                    .authorities(role)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();
        } catch (Exception e) {
            throw new UsernameNotFoundException("用户未找到: " + username);
        }
    }
}
