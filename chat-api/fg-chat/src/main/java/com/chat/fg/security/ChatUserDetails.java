package com.chat.fg.security;

import com.xy.common.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * 聊天客户端用户 UserDetails（基于 t_user 表）
 * 与 LoginUser（基于 sys_user 表）完全隔离
 */
public class ChatUserDetails implements UserDetails {

    private static final long serialVersionUID = 10000000L;

    private final User user;

    public ChatUserDetails(User user) {
        this.user = user;
    }

    public Long getUId() {
        return user.getUId();
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // 格式: chat_{uId}，与管理后台 SysUser 的 username 区分
        return "chat_" + user.getUId();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "0".equals(user.getStatus());
    }
}