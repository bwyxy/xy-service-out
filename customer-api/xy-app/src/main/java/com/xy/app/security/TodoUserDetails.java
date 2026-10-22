package com.xy.app.security;

import com.xy.common.constant.Constants;
import com.xy.common.entity.User;
import com.xy.common.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;

/**
 * 待办客户端用户 UserDetails（基于 t_user 表）
 *
 * <p>与后台管理端的 LoginUser（基于 sys_user 表）完全隔离，username 统一为 {@code todo_{uId}}，
 * 使 JwtAuthenticationFilter 能按前缀区分两套用户体系，避免待办 token 与后台 token 互相可用。</p>
 * */
public class TodoUserDetails implements UserDetails {

    @Serial
    private static final long serialVersionUID = 10000000L;

    /** 待办用户 username 前缀，与后台管理端 sys_user 的 username 区分 */
    public static final String USERNAME_PREFIX = "todo_";

    private final User user;

    public TodoUserDetails(User user) {
        this.user = user;
    }

    /**
     * 由 UID 生成 token 主题，如 todo_10000001
     * */
    public static String toUsername(Long uId) {
        return USERNAME_PREFIX + uId;
    }

    /**
     * 当前待办登录用户的 u_id
     *
     * <p>待办登录态的主体是 TodoUserDetails，不能用 SecurityUtils.getUserId()（那是后台 sys_user 体系），
     * 未登录或非待办登录态统一按 401 处理。</p>
     * */
    public static Long getCurrentUId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof TodoUserDetails todoUserDetails)) {
            throw new ServiceException(Constants.HttpStatus.UNAUTHORIZED, "未登录或登录已过期");
        }

        return todoUserDetails.getUId();
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
        return toUsername(user.getUId());
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
        // t_user.status：0-正常 1-禁用
        return "0".equals(user.getStatus());
    }
}
