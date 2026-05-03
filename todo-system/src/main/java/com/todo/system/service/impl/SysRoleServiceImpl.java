package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysRole;
import com.todo.system.mapper.SysRoleMapper;
import com.todo.system.service.ISysRoleService;
import org.springframework.stereotype.Service;
import com.todo.common.utils.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    /**
     * 根据条件分页查询角色数据
     *
     * @param pageNum  当前页
     * @param pageSize 每页条数
     * @param role     角色信息
     * @return 角色数据集合信息
     */
    @Override
    public PageResult<SysRole> selectRolePage(int pageNum, int pageSize, SysRole role) {
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();

        if (role != null) {
            if (StringUtils.hasText(role.getRoleName())) {
                queryWrapper.like(SysRole::getRoleName, role.getRoleName());
            }
            if (StringUtils.hasText(role.getRoleKey())) {
                queryWrapper.eq(SysRole::getRoleKey, role.getRoleKey());
            }
            if (StringUtils.hasText(role.getStatus())) {
                queryWrapper.eq(SysRole::getStatus, role.getStatus());
            }
            if (StringUtils.hasText(role.getDataScope())) {
                queryWrapper.eq(SysRole::getDataScope, role.getDataScope());
            }
        }

        queryWrapper.orderByAsc(SysRole::getRoleSort);

        Page<SysRole> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据用户ID查询角色列表
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRolesByUserId(Long userId) {
        return this.baseMapper.selectRolesByUserId(userId);
    }

    /**
     * 根据用户 ID查询权限
     */
    public Set<String> selectRolePermissionByUserId(Long userId) {
        List<SysRole> roles = this.baseMapper.selectRolesByUserId(userId);

        Set<String> permsSet = new HashSet<>();

        for (SysRole role : roles) {
            if (StringUtils.isNotNull(role)) {
                permsSet.addAll(Arrays.asList(role.getRoleKey().trim().split(",")));
            }
        }

        return permsSet;
    }
}
