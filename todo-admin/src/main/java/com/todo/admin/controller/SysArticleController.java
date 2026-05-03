package com.todo.admin.controller;

import com.todo.common.core.domain.PageResult;
import com.todo.common.core.domain.R;
import com.todo.common.entity.SysArticle;
import com.todo.system.service.ISysArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * */
@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class SysArticleController {

    @Autowired
    private ISysArticleService sysArticleService;

    /**
     * 分页查询文章列表
     * */
    @PostMapping("/list")
    public R<?> selectSysArticleList(@RequestBody SysArticle sysArticle) {

        PageResult<SysArticle> pageResult = sysArticleService.selectSysArticleList(sysArticle);

        return R.ok(pageResult);
    }
}
