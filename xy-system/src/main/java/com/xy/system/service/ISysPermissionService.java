package com.xy.system.service;

import com.xy.common.entity.SysUser;

import java.util.Set;

public interface ISysPermissionService {

    /**
     * 获取角色数据权限
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    public Set<String> getRolePermission(SysUser user);
}
