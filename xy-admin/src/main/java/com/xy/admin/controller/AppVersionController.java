package com.xy.admin.controller;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.AppVersion;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.ISysAppVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 应用版本管理
 * */
@RestController
@RequestMapping("/system/app-version")
@RequiredArgsConstructor
public class AppVersionController {

    private final ISysAppVersionService appVersionService;

    /**
     * 查询版本列表
     * */
    @PreAuthorize("@auth.hasPermi('system:appVersion:list')")
    @PostMapping("/list")
    public R<PageResult<AppVersion>> list(@RequestBody AppVersion query) {
        return R.ok(appVersionService.selectAppVersionList(query));
    }

    /**
     * 查询版本详情
     * */
    @PreAuthorize("@auth.hasPermi('system:appVersion:list')")
    @GetMapping("/{id}")
    public R<AppVersion> getInfo(@PathVariable Long id) {
        return R.ok(appVersionService.getById(id));
    }

    /**
     * 新增版本
     * */
    @PreAuthorize("@auth.hasPermi('system:appVersion:add')")
    @Log(title = "应用版本", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody AppVersion appVersion) {
        return R.ok(appVersionService.insertAppVersion(appVersion));
    }

    /**
     * 修改版本
     * */
    @PreAuthorize("@auth.hasPermi('system:appVersion:edit')")
    @Log(title = "应用版本", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody AppVersion appVersion) {
        appVersionService.updateAppVersion(appVersion);
        return R.ok();
    }

    /**
     * 删除版本
     * */
    @PreAuthorize("@auth.hasPermi('system:appVersion:remove')")
    @Log(title = "应用版本", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public R<Void> remove(@PathVariable Long id) {
        appVersionService.removeById(id);
        return R.ok();
    }

    /**
     * 为已存在的版本记录上传/替换安装包
     * */
    @PreAuthorize("@auth.hasPermi('system:appVersion:upload')")
    @Log(title = "应用版本", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/package")
    public R<String> uploadPackage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return R.ok(appVersionService.uploadPackage(id, file));
    }
}
