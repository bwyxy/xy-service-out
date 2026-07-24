package com.xy.system.service.impl;

import com.xy.common.constant.Constants;
import com.xy.common.entity.SysUser;
import com.xy.system.service.ISysPermissionService;
import com.xy.system.service.ISysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * 用户权限处理
 * */
@Component
public class SysPermissionServiceImpl implements ISysPermissionService {

    @Autowired
    private ISysRoleService sysRoleService;

    /**
     * 获取角色数据权限
     *
     * @param user 用户信息
     * @return 角色权限信息
     */
    @Override
    public Set<String> getRolePermission(SysUser user) {
        Set<String> roleSet = new HashSet<>();

        if (user.isAdmin()) {
            roleSet.add(Constants.SUPER_ADMIN);
        } else {
            roleSet.addAll(sysRoleService.selectRolePermissionByUserId(user.getId()));
        }

        return roleSet;
    }
}
