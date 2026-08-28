package com.xy.admin.controller;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.SysConfig;
import com.xy.system.service.ISysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 内容配置
 * */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class SysConfigController {

    @Autowired
    private ISysConfigService sysConfigService;

    /**
     * 查询配置列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody SysConfig sysConfig) {
        PageResult<SysConfig> sysConfigs = sysConfigService.findSysConfigs(sysConfig);
        return R.ok(sysConfigs);
    }

    /**
     * 根据配置 Id查询配置信息
     * */
    @GetMapping("/{id}")
    public R<SysConfig> getById(@PathVariable Long id) {
        SysConfig sysConfig = sysConfigService.findSysConfigById(id);

        return R.ok(sysConfig);
    }

    /**
     * 添加配置
     * */
    @PostMapping("/add")
    public R<?> add(@RequestBody SysConfig sysConfig) {
        sysConfigService.saveSysConfig(sysConfig);

        return R.ok("添加成功");
    }

    /**
     * 更新配置
     * */
    @PutMapping("/edit")
    public R<?> edit(@RequestBody SysConfig sysConfig) {
        sysConfigService.updateSysConfig(sysConfig);

        return R.ok("更新成功");
    }

    /**
     * 根据配置 Id 删除配置
     * */
    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        sysConfigService.deleteSysConfigById(id);

        return R.ok("删除成功");
    }
}
