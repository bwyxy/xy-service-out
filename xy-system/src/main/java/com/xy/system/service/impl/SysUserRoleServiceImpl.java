package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.system.mapper.SysUserRoleMapper;
import com.xy.system.service.ISysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户与角色关联服务实现
 * */
@Service
@RequiredArgsConstructor
public class SysUserRoleServiceImpl implements ISysUserRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<Long> selectRoleIdsByUserId(Long userId) {
        return sysUserRoleMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserRole(Long userId, List<Long> roleIds) {
        deleteUserRoleByUserId(userId);
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        sysUserRoleMapper.batchInsert(userId, roleIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserRoleByUserId(Long userId) {
        sysUserRoleMapper.deleteByUserId(userId);
    }

    @Override
    public long countUserByRoleId(Long roleId) {
        return sysUserRoleMapper.countByRoleId(roleId);
    }
}
