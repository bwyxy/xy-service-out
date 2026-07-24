package com.xy.admin.controller;

import com.xy.common.core.domain.R;
import com.xy.common.entity.SysBlogConfig;
import com.xy.system.service.ISysBlogConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class SysBlogConfigController {

    @Autowired
    private ISysBlogConfigService sysBlogConfigService;

    /**
     * 查询博客配置列表
     * */
    @PostMapping("/list")
    public R<List<SysBlogConfig>> list(@RequestBody SysBlogConfig sysBlogConfig) {

        List<SysBlogConfig> blogConfigs = sysBlogConfigService.selectBlogConfigList(sysBlogConfig);

        return R.ok(blogConfigs);
    }

    /**
     * 根据配置 Id查询配置信息
     * */
    @GetMapping("/{id}")
    public R<SysBlogConfig> getConfig(@PathVariable Long id) {

        return R.ok(sysBlogConfigService.selectBlogConfigById(id));
    }

    /**
     * 新增配置信息
     * */
    @PostMapping("/add")
    public R<?> addConfig(@RequestBody SysBlogConfig sysBlogConfig) {

        sysBlogConfigService.saveBlogConfig(sysBlogConfig);

        return R.ok("新增成功");
    }

    /**
     * 更新配置信息
     * */
    @PutMapping("/update")
    public R<?> updateConfig(@RequestBody SysBlogConfig sysBlogConfig) {
        sysBlogConfigService.updateBlogConfig(sysBlogConfig);

        return R.ok("更新成功");
    }
}
