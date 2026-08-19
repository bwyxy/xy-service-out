package com.xy.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysRole;
import com.xy.common.entity.SysUser;

import java.util.List;
import java.util.Set;

public interface ISysRoleService extends IService<SysRole> {

    /**
     * 根据条件分页查询角色数据
     *
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param role     角色信息
     * @return 角色数据集合信息
     */
    PageResult<SysRole> selectRolePage(int pageNum, int pageSize, SysRole role);

    /**
     * 根据用户 ID查询角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    List<SysRole> selectRolesByUserId(Long userId);

    /**
     * 根据用户 ID查询权限
     */
    Set<String> selectRolePermissionByUserId(Long userId);

    /**
     * 根据角色ID查询角色信息（含菜单ID）
     * @param roleId 角色ID
     * @return 角色信息
     * */
    SysRole selectRoleById(Long roleId);

    /**
     * 新增角色
     * @param role 角色信息
     * @return 角色ID
     * */
    Long insertRole(SysRole role);

    /**
     * 修改角色
     * @param role 角色信息
     * @return 是否成功
     * */
    boolean updateRole(SysRole role);

    /**
     * 删除角色
     * @param roleId 角色ID
     * @return 是否成功
     * */
    boolean deleteRole(Long roleId);

    /**
     * 校验角色名称是否唯一
     * @param role 角色信息
     * @return true 唯一
     * */
    boolean checkRoleNameUnique(SysRole role);

    /**
     * 校验角色权限字符串是否唯一
     * @param role 角色信息
     * @return true 唯一
     * */
    boolean checkRoleKeyUnique(SysRole role);

    /**
     * 判断是否为超级管理员角色
     * @param role 角色信息
     * @return true 是
     * */
    boolean isSuperAdminRole(SysRole role);

    /**
     * 判断是否为超级管理员角色
     * @param roleId 角色ID
     * @return true 是
     * */
    boolean isSuperAdminRole(Long roleId);
}
