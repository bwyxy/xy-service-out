package com.xy.system.service;

import java.util.List;
import java.util.Set;

/**
 * 角色与菜单关联服务接口
 * */
public interface ISysRoleMenuService {

    /**
     * 根据角色ID查询菜单ID列表
     * @param roleId 角色ID
     * @return 菜单ID列表
     * */
    List<Long> selectMenuIdsByRoleId(Long roleId);

    /**
     * 保存角色菜单关联
     * @param roleId 角色ID
     * @param menuIds 菜单ID列表
     * */
    void saveRoleMenu(Long roleId, List<Long> menuIds);

    /**
     * 根据角色ID删除菜单关联
     * @param roleId 角色ID
     * */
    void deleteRoleMenuByRoleId(Long roleId);

    /**
     * 根据菜单ID统计关联角色数
     * @param menuId 菜单ID
     * @return 角色数
     * */
    long countRoleByMenuId(Long menuId);

    /**
     * 根据用户ID查询权限标识集合
     * @param userId 用户ID
     * @return 权限标识集合
     * */
    Set<String> selectMenuPermsByUserId(Long userId);
}
