package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.chat.LoginLog;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.IUserLoginLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 客户端用户登录日志管理
 *
 * <p>仅记录聊天端用户（t_user）登录，与后台账号登录日志相互独立。</p>
 */
@RestController
@RequestMapping("/system/login-log")
@RequiredArgsConstructor
public class UserLoginLogController {

    private final IUserLoginLogService userLoginLogService;

    /**
     * 查询登录日志列表
     */
    @PreAuthorize("@auth.hasPermi('system:loginlog:list')")
    @PostMapping("/list")
    public R<PageResult<LoginLog>> list(@RequestBody LoginLog query) {
        return R.ok(userLoginLogService.selectLoginLogPage(query));
    }

    /**
     * 删除登录日志
     */
    @PreAuthorize("@auth.hasPermi('system:loginlog:remove')")
    @Log(title = "客户端登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        userLoginLogService.deleteLoginLogById(id);
        return R.ok();
    }
}
