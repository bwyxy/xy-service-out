package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysUser;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 系统用户管理
 * */
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final ISysUserService sysUserService;

    /**
     * 查询管理用户列表
     * */
    @PreAuthorize("@auth.hasPermi('system:user:list')")
    @PostMapping("/list")
    public R<PageResult<SysUser>> list(@RequestBody SysUser sysUser) {
        PageResult<SysUser> result = sysUserService.selectUserList(sysUser);
        return R.ok(result);
    }

    /**
     * 根据用户ID查询详情
     * */
    @PreAuthorize("@auth.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public R<SysUser> getInfo(@PathVariable Long userId) {
        return R.ok(sysUserService.selectUserById(userId));
    }

    /**
     * 新增用户
     * */
    @PreAuthorize("@auth.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody SysUser sysUser) {
        Long userId = sysUserService.insertUser(sysUser);
        return R.ok(userId);
    }

    /**
     * 修改用户
     * */
    @PreAuthorize("@auth.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysUser sysUser) {
        sysUserService.updateUser(sysUser);
        return R.ok();
    }

    /**
     * 删除用户
     * */
    @PreAuthorize("@auth.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userId}")
    public R<Void> remove(@PathVariable Long userId) {
        sysUserService.deleteUser(userId);
        return R.ok();
    }
}
