package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysRole;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 角色管理
 * */
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final ISysRoleService roleService;

    /**
     * 查询角色列表
     * */
    @PreAuthorize("@auth.hasPermi('system:role:list')")
    @PostMapping("/list")
    public R<PageResult<SysRole>> list(@RequestBody SysRole role) {
        int pageNum = role.getPageNum() <= 0 ? 1 : role.getPageNum();
        int pageSize = role.getPageSize() <= 0 ? 10 : role.getPageSize();
        return R.ok(roleService.selectRolePage(pageNum, pageSize, role));
    }

    /**
     * 根据角色ID查询详情
     * */
    @PreAuthorize("@auth.hasPermi('system:role:query')")
    @GetMapping("/{roleId}")
    public R<SysRole> getInfo(@PathVariable Long roleId) {
        return R.ok(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     * */
    @PreAuthorize("@auth.hasPermi('system:role:add')")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody SysRole role) {
        return R.ok(roleService.insertRole(role));
    }

    /**
     * 修改角色
     * */
    @PreAuthorize("@auth.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody SysRole role) {
        roleService.updateRole(role);
        return R.ok();
    }

    /**
     * 删除角色
     * */
    @PreAuthorize("@auth.hasPermi('system:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleId}")
    public R<Void> remove(@PathVariable Long roleId) {
        roleService.deleteRole(roleId);
        return R.ok();
    }
}
