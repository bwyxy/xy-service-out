package com.xy.system.service;

import java.util.List;

/**
 * 用户与角色关联服务接口
 * */
public interface ISysUserRoleService {

    /**
     * 根据用户ID查询角色ID列表
     * @param userId 用户ID
     * @return 角色ID列表
     * */
    List<Long> selectRoleIdsByUserId(Long userId);

    /**
     * 保存用户角色关联
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     * */
    void saveUserRole(Long userId, List<Long> roleIds);

    /**
     * 根据用户ID删除角色关联
     * @param userId 用户ID
     * */
    void deleteUserRoleByUserId(Long userId);

    /**
     * 根据角色ID统计关联用户数
     * @param roleId 角色ID
     * @return 用户数
     * */
    long countUserByRoleId(Long roleId);
}
