package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.constant.Constants;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysRole;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.constant.UserConstants;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysRoleMapper;
import com.xy.system.service.ISysRoleMenuService;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.ISysUserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {

    private final ISysRoleMenuService roleMenuService;
    private final ISysUserRoleService userRoleService;

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

    @Override
    public SysRole selectRoleById(Long roleId) {
        SysRole role = getById(roleId);
        if (role == null) {
            return null;
        }
        List<Long> menuIds = roleMenuService.selectMenuIdsByRoleId(roleId);
        role.setMenuIds(menuIds);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertRole(SysRole role) {
        validateRole(role);
        if (!checkRoleNameUnique(role)) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色名称已存在");
        }
        if (!checkRoleKeyUnique(role)) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色权限字符串已存在");
        }
        if (StringUtils.isEmpty(role.getStatus())) {
            role.setStatus(UserConstants.ROLE_NORMAL);
        }
        boolean success = save(role);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "新增角色失败");
        }
        if (role.getMenuIds() != null && !role.getMenuIds().isEmpty()) {
            roleMenuService.saveRoleMenu(role.getId(), role.getMenuIds());
        }
        return role.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateRole(SysRole role) {
        SysRole existing = getById(role.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        validateRole(role);
        if (StringUtils.hasText(role.getRoleName()) && !role.getRoleName().equals(existing.getRoleName())) {
            if (!checkRoleNameUnique(role)) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色名称已存在");
            }
        }
        if (StringUtils.hasText(role.getRoleKey()) && !role.getRoleKey().equals(existing.getRoleKey())) {
            if (!checkRoleKeyUnique(role)) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "角色权限字符串已存在");
            }
        }
        if (isSuperAdminRole(role.getId()) && UserConstants.ROLE_DISABLE.equals(role.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "禁止停用超级管理员角色");
        }

        boolean success = updateById(role);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "修改角色失败");
        }
        if (role.getMenuIds() != null) {
            roleMenuService.saveRoleMenu(role.getId(), role.getMenuIds());
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteRole(Long roleId) {
        SysRole existing = getById(roleId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "角色不存在");
        }
        if (isSuperAdminRole(roleId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "禁止删除超级管理员角色");
        }
        long userCount = userRoleService.countUserByRoleId(roleId);
        if (userCount > 0) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "该角色已分配用户，无法删除");
        }
        boolean success = getBaseMapper().deleteById(roleId) > 0;
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "删除角色失败");
        }
        roleMenuService.deleteRoleMenuByRoleId(roleId);
        return true;
    }

    @Override
    public boolean checkRoleNameUnique(SysRole role) {
        Long roleId = role.getId() == null ? -1L : role.getId();
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getRoleName, role.getRoleName());
        SysRole existing = getOne(queryWrapper);
        return existing == null || existing.getId().equals(roleId);
    }

    @Override
    public boolean checkRoleKeyUnique(SysRole role) {
        Long roleId = role.getId() == null ? -1L : role.getId();
        LambdaQueryWrapper<SysRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysRole::getRoleKey, role.getRoleKey());
        SysRole existing = getOne(queryWrapper);
        return existing == null || existing.getId().equals(roleId);
    }

    @Override
    public boolean isSuperAdminRole(SysRole role) {
        return role != null && Constants.SUPER_ADMIN.equals(role.getRoleKey());
    }

    @Override
    public boolean isSuperAdminRole(Long roleId) {
        SysRole role = getById(roleId);
        return isSuperAdminRole(role);
    }

    private void validateRole(SysRole role) {
        if (StringUtils.isEmpty(role.getRoleName())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "角色名称不能为空");
        }
        if (StringUtils.isEmpty(role.getRoleKey())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "角色权限字符串不能为空");
        }
    }
}
