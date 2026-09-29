package com.xy.blog.controller;

import com.xy.blog.service.SysBlogConfigService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 博客相关配置
 * */
@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogConfigController {

    @Autowired
    private SysBlogConfigService sysBlogConfigService;

    /**
     * 获取博客基础配置
     * */
    @Anonymous
    @PostMapping("/config")
    public R<?> getBlogConfig() {
        return R.ok(sysBlogConfigService.getAllConfigs());
    }

    /**
     * 查询公告
     * */
    @Anonymous
    @GetMapping("/notice")
    public R<?> getNoticeList() {
        return R.ok(sysBlogConfigService.getNoticeList());
    }

    @GetMapping("/test")
    public R<?> test() {
        return R.ok("test");
    }
}
