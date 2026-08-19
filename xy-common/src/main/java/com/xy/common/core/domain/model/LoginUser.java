package com.xy.common.core.domain.model;

import com.alibaba.fastjson.annotation.JSONField;
import com.xy.common.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * 登录用户身份权限
 * */
public class LoginUser implements UserDetails {

    @Serial
    private static final long serialVersionUID = 10000000L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户唯一标识
     */
    private String token;

    /**
     * 登录时间
     */
    private Long loginTime;

    /**
     * 过期时间
     */
    private Long expireTime;

    /**
     * 登录IP地址
     */
    private String ipaddr;

    /**
     * 登录地点
     */
    private String loginLocation;

    /**
     * 浏览器类型
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 权限列表
     */
    private Set<String> permissions;

    /**
     * 用户
     */
    private SysUser sysUser;

    public LoginUser(SysUser user)
    {
        this.sysUser = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public String getPassword() {
        return sysUser.getPassword();
    }

    @JSONField(serialize = false)
    @Override
    public String getUsername() {
        return sysUser.getUsername();
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public Long getExpireTime() {
        return expireTime;
    }
    public void setExpireTime(Long expireTime) {
        this.expireTime = expireTime;
    }

    public Long getLoginTime() {
        return loginTime;
    }
    public void setLoginTime(Long loginTime) {
        this.loginTime = loginTime;
    }

    public String getIpaddr() {
        return ipaddr;
    }
    public void setIpaddr(String ipaddr) {
        this.ipaddr = ipaddr;
    }

    public String getLoginLocation() {
        return loginLocation;
    }
    public void setLoginLocation(String loginLocation) {
        this.loginLocation = loginLocation;
    }

    public String getBrowser() {
        return browser;
    }
    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOs() {
        return os;
    }
    public void setOs(String os) {
        this.os = os;
    }

    /**
     * 账户是否未过期,过期无法验证
     */
    @JSONField(serialize = false)
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 指定用户是否解锁,锁定的用户无法进行身份验证
     * */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 指示是否已过期的用户的凭据(密码),过期的凭据防止认证
     * */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 是否可用 ,禁用的用户不能身份验证
     * */
    @JSONField(serialize = false)
    @Override
    public boolean isEnabled() {
        return "0".equals(sysUser.getStatus());
    }

    public Long getUserId() {
        return sysUser.getId();
    }

    public SysUser getSysUser() {
        return sysUser;
    }
}
