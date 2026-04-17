package com.todo.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysRole;
import com.todo.common.entity.SysUser;

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
    public Set<String> selectRolePermissionByUserId(Long userId);
}
