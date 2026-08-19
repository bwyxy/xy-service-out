package com.xy.system.service.impl;

import com.xy.system.mapper.SysRoleMenuMapper;
import com.xy.system.service.ISysRoleMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色与菜单关联服务实现
 * */
@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl implements ISysRoleMenuService {

    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    public List<Long> selectMenuIdsByRoleId(Long roleId) {
        return sysRoleMenuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleMenu(Long roleId, List<Long> menuIds) {
        deleteRoleMenuByRoleId(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        sysRoleMenuMapper.batchInsert(roleId, menuIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleMenuByRoleId(Long roleId) {
        sysRoleMenuMapper.deleteByRoleId(roleId);
    }

    @Override
    public long countRoleByMenuId(Long menuId) {
        return sysRoleMenuMapper.countByMenuId(menuId);
    }

    @Override
    public Set<String> selectMenuPermsByUserId(Long userId) {
        Set<String> perms = sysRoleMenuMapper.selectMenuPermsByUserId(userId);
        return perms == null ? new HashSet<>() : perms;
    }
}
