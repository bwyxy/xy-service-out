package com.todo.blog.controller;

import com.todo.blog.service.SysBlogConfigService;
import com.todo.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/web/blog")
@RequiredArgsConstructor
public class BlogConfigController {

    @Autowired
    private SysBlogConfigService sysBlogConfigService;

//    @PreAuthorize("permitAll()")
    @PostMapping("/config")
    public R<?> getBlogConfig() {
        return R.ok(sysBlogConfigService.getAllConfigs());
    }

    @GetMapping("/test")
    public R<?> test() {
        return R.ok("test");
    }
}
