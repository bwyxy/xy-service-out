package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysLogininfor;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysLogininforService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 登录日志管理 控制层
 * */
@RestController
@RequestMapping("/system/logininfor")
@RequiredArgsConstructor
public class SysLogininforController {

    private final ISysLogininforService logininforService;

    /**
     * 查询登录日志列表
     * */
    @PreAuthorize("@auth.hasPermi('system:logininfor:list')")
    @PostMapping("/list")
    public R<PageResult<SysLogininfor>> list(@RequestBody SysLogininfor logininfor) {
        return R.ok(logininforService.selectLogininforPage(logininfor));
    }

    /**
     * 删除登录日志
     * */
    @PreAuthorize("@auth.hasPermi('system:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoId}")
    public R<Void> remove(@PathVariable Long infoId) {
        logininforService.deleteLogininforById(infoId);
        return R.ok();
    }

    /**
     * 清空登录日志
     * */
    @PreAuthorize("@auth.hasPermi('system:logininfor:clean')")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public R<Void> clean() {
        logininforService.cleanLogininfor();
        return R.ok();
    }

    /**
     * 导出登录日志
     * */
    @PreAuthorize("@auth.hasPermi('system:logininfor:export')")
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, @RequestBody SysLogininfor logininfor) {
        logininforService.exportLogininfor(logininfor, response);
    }
}
