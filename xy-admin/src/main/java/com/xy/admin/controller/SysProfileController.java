package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.R;
import com.xy.common.dto.UpdatePasswordDTO;
import com.xy.common.entity.SysUser;
import com.xy.common.enums.BusinessType;
import com.xy.common.utils.SecurityUtils;
import com.xy.system.service.ISysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人信息
 * */
@RestController
@RequestMapping("/system/user/profile")
@RequiredArgsConstructor
public class SysProfileController {

    private final ISysUserService sysUserService;

    /**
     * 个人信息
     * */
    @GetMapping
    public R<SysUser> profile() {
        SysUser user = sysUserService.selectUserById(SecurityUtils.getUserId());
        if (user != null) {
            user.setPassword(null);
        }
        return R.ok(user);
    }

    /**
     * 修改个人资料（昵称、性别、邮箱、手机号、头像、简介）
     * */
    @Log(title = "个人资料", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> updateProfile(@RequestBody SysUser sysUser) {
        // 只允许修改自己的资料，忽略前端传入的敏感字段
        sysUser.setId(SecurityUtils.getUserId());
        sysUser.setUsername(null);
        sysUser.setPassword(null);
        sysUser.setStatus(null);
        sysUser.setRoleIds(null);
        sysUserService.updateUserProfile(sysUser);
        return R.ok();
    }

    /**
     * 修改账号密码
     * */
    @Log(title = "修改密码", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public R<Void> updatePwd(@Valid @RequestBody UpdatePasswordDTO dto) {
        sysUserService.updateUserPwd(SecurityUtils.getUserId(), dto.getOldPassword(), dto.getNewPassword());
        return R.ok();
    }
}
