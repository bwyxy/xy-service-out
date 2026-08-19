package com.xy.system.service.impl;

import com.xy.common.constant.Constants;
import com.xy.common.entity.SysRole;
import com.xy.common.entity.SysUser;
import com.xy.common.utils.SecurityUtils;
import com.xy.system.service.ISysRoleMenuService;
import com.xy.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 权限校验服务（供 Spring EL 表达式使用，bean 名称为 auth）
 * */
@Service("auth")
public class PermissionServiceImpl {

    @Autowired
    private ISysRoleMenuService sysRoleMenuService;

    @Autowired
    private ISysRoleService sysRoleService;

    /**
     * 验证用户是否具备某权限
     * @param permission 权限字符串
     * @return true 有权限
     * */
    public boolean hasPermi(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }
        Set<String> permissions = getPermissions();
        return permissions.contains("*:*:*") || permissions.contains(permission);
    }

    /**
     * 验证用户是否含有指定权限，只需包含其中一个
     * */
    public boolean hasPermiOr(String... permissions) {
        if (permissions == null || permissions.length == 0) {
            return false;
        }
        Set<String> userPermissions = getPermissions();
        for (String permission : permissions) {
            if (userPermissions.contains("*:*:*") || userPermissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证用户是否具备某角色
     * @param role 角色字符串
     * @return true 有角色
     * */
    public boolean hasRole(String role) {
        if (role == null || role.isEmpty()) {
            return false;
        }
        Set<String> roles = getRoles();
        return roles.contains(Constants.SUPER_ADMIN) || roles.contains(role);
    }

    /**
     * 验证用户是否含有指定角色，只需包含其中一个
     * */
    public boolean hasRoleOr(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }
        Set<String> userRoles = getRoles();
        for (String role : roles) {
            if (userRoles.contains(Constants.SUPER_ADMIN) || userRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getPermissions() {
        Set<String> permissions = new HashSet<>();
        SysUser user = SecurityUtils.getLoginUser().getSysUser();
        if (user == null) {
            return permissions;
        }
        if (user.isAdmin()) {
            permissions.add("*:*:*");
            return permissions;
        }
        return sysRoleMenuService.selectMenuPermsByUserId(user.getId());
    }

    private Set<String> getRoles() {
        Set<String> roles = new HashSet<>();
        SysUser user = SecurityUtils.getLoginUser().getSysUser();
        if (user == null) {
            return roles;
        }
        if (user.isAdmin()) {
            roles.add(Constants.SUPER_ADMIN);
            return roles;
        }
        List<SysRole> roleList = sysRoleService.selectRolesByUserId(user.getId());
        for (SysRole role : roleList) {
            if (role.getRoleKey() != null) {
                roles.add(role.getRoleKey());
            }
        }
        return roles;
    }
}
