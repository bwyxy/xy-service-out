package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysRole;
import com.xy.common.entity.SysUser;
import com.xy.common.constant.UserConstants;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysUserMapper;
import com.xy.system.service.ISysRoleService;
import com.xy.system.service.ISysUserRoleService;
import com.xy.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private static final String ADMIN_ROLE_KEY = "admin";

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ISysRoleService roleService;
    private final ISysUserRoleService userRoleService;

    @Override
    public SysUser getByUsername(String username) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, username);
        return this.getOne(queryWrapper);
    }

    @Override
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Override
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * 根据 openid 查询用户信息（返回完整用户对象）
     * @param openId 微信小程序用户唯一标识
     * @return 用户对象，不存在则返回 null
     * */
    @Override
    public SysUser getUserByOpenId(String openId) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysUser::getWxOpenId, openId);

        return this.getOne(queryWrapper);
    }

    /**
     * 添加用户
     * */
    @Override
    public Long addUser(SysUser sysUser) {

        boolean success = save(sysUser);

        if (success) {
            Long userId = sysUser.getId();

            if (StringUtils.isEmpty(sysUser.getUsername())) {
                String username = String.valueOf(userId);
                sysUser.setUsername(username);
                this.lambdaUpdate()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getUsername, username)
                        .update();
            }

            return sysUser.getId();
        }

        throw new RuntimeException("添加用户失败");
    }

    /**
     * 分页查询管理用户列表
     * */
    @Override
    public PageResult<SysUser> selectUserList(SysUser user) {
        int pageNum = user.getPageNum() <= 0 ? 1 : user.getPageNum();
        int pageSize = user.getPageSize() <= 0 ? 10 : user.getPageSize();

        Page<SysUser> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.isNotEmpty(user.getUsername())) {
            queryWrapper.like(SysUser::getUsername, user.getUsername());
        }
        if (StringUtils.isNotEmpty(user.getNickname())) {
            queryWrapper.like(SysUser::getNickname, user.getNickname());
        }
        if (StringUtils.isNotEmpty(user.getPhone())) {
            queryWrapper.like(SysUser::getPhone, user.getPhone());
        }
        if (StringUtils.isNotEmpty(user.getStatus())) {
            queryWrapper.eq(SysUser::getStatus, user.getStatus());
        }

        page = this.page(page, queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 新增管理用户
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertUser(SysUser sysUser) {
        if (!checkUsernameUnique(sysUser)) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }
        validateUser(sysUser);

        if (StringUtils.isEmpty(sysUser.getPassword())) {
            sysUser.setPassword(encodePassword("123456"));
        } else {
            sysUser.setPassword(encodePassword(sysUser.getPassword()));
        }

        if (StringUtils.isEmpty(sysUser.getStatus())) {
            sysUser.setStatus(UserConstants.NORMAL);
        }

        boolean success = save(sysUser);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "新增用户失败");
        }

        Long userId = sysUser.getId();
        if (sysUser.getRoleIds() != null && !sysUser.getRoleIds().isEmpty()) {
            userRoleService.saveUserRole(userId, sysUser.getRoleIds());
        }
        return userId;
    }

    /**
     * 修改管理用户
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser sysUser) {
        SysUser existing = getById(sysUser.getId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }

        if (isSuperAdmin(sysUser.getId()) && UserConstants.USER_DISABLE.equals(sysUser.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "禁止停用超级管理员账号");
        }

        if (StringUtils.isNotEmpty(sysUser.getUsername()) && !sysUser.getUsername().equals(existing.getUsername())) {
            if (!checkUsernameUnique(sysUser)) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "用户名已存在");
            }
        }
        validateUser(sysUser);

        if (StringUtils.isNotEmpty(sysUser.getPassword())) {
            sysUser.setPassword(encodePassword(sysUser.getPassword()));
        } else {
            sysUser.setPassword(null);
        }

        boolean success = updateById(sysUser);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "修改用户失败");
        }

        if (sysUser.getRoleIds() != null) {
            userRoleService.saveUserRole(sysUser.getId(), sysUser.getRoleIds());
        }
        return true;
    }

    /**
     * 删除管理用户
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteUser(Long userId) {
        SysUser existing = getById(userId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "用户不存在");
        }
        if (isSuperAdmin(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "禁止删除超级管理员账号");
        }

        boolean success = getBaseMapper().deleteById(userId) > 0;
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "删除用户失败");
        }
        userRoleService.deleteUserRoleByUserId(userId);
        return true;
    }

    /**
     * 根据用户ID查询用户信息（含角色ID）
     * */
    @Override
    public SysUser selectUserById(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            return null;
        }
        List<Long> roleIds = userRoleService.selectRoleIdsByUserId(userId);
        user.setRoleIds(roleIds);
        return user;
    }

    /**
     * 校验用户名是否唯一
     * */
    @Override
    public boolean checkUsernameUnique(SysUser sysUser) {
        Long userId = sysUser.getId() == null ? -1L : sysUser.getId();
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysUser::getUsername, sysUser.getUsername());
        SysUser existing = getOne(queryWrapper);
        return existing == null || existing.getId().equals(userId);
    }

    /**
     * 校验用户是否拥有超级管理员角色
     * */
    @Override
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        if (roles == null || roles.isEmpty()) {
            return false;
        }
        return roles.stream()
                .anyMatch(role -> ADMIN_ROLE_KEY.equals(role.getRoleKey()));
    }

    /**
     * 用户基础数据校验
     * */
    private void validateUser(SysUser sysUser) {
        if (StringUtils.isEmpty(sysUser.getUsername())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "用户名不能为空");
        }
        if (sysUser.getUsername().length() < UserConstants.USERNAME_MIN_LENGTH
                || sysUser.getUsername().length() > UserConstants.USERNAME_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                    "用户名长度必须在" + UserConstants.USERNAME_MIN_LENGTH + "-" + UserConstants.USERNAME_MAX_LENGTH + "之间");
        }
        if (StringUtils.isNotEmpty(sysUser.getPassword())
                && (sysUser.getPassword().length() < UserConstants.PASSWORD_MIN_LENGTH
                || sysUser.getPassword().length() > UserConstants.PASSWORD_MAX_LENGTH)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR,
                    "密码长度必须在" + UserConstants.PASSWORD_MIN_LENGTH + "-" + UserConstants.PASSWORD_MAX_LENGTH + "之间");
        }
    }
}
