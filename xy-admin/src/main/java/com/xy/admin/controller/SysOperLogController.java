package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysOperLog;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysOperLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 操作日志管理 控制层
 * */
@RestController
@RequestMapping("/system/operlog")
@RequiredArgsConstructor
public class SysOperLogController {

    private final ISysOperLogService operLogService;

    /**
     * 查询操作日志列表
     * */
    @PreAuthorize("@auth.hasPermi('system:operlog:list')")
    @PostMapping("/list")
    public R<PageResult<SysOperLog>> list(@RequestBody SysOperLog operLog) {
        return R.ok(operLogService.selectOperLogPage(operLog));
    }

    /**
     * 删除操作日志
     * */
    @PreAuthorize("@auth.hasPermi('system:operlog:remove')")
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{operId}")
    public R<Void> remove(@PathVariable Long operId) {
        operLogService.removeById(operId);
        return R.ok();
    }

    /**
     * 清空操作日志
     * */
    @PreAuthorize("@auth.hasPermi('system:operlog:clean')")
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public R<Void> clean() {
        operLogService.cleanOperLog();
        return R.ok();
    }
}
