package com.todo.admin.controller;

import com.todo.common.core.domain.PageResult;
import com.todo.common.core.domain.R;
import com.todo.common.entity.SysArticle;
import com.todo.system.service.ISysArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 根据文章 Id 查询文章信息
     * */
    @GetMapping("/{id}")
    public R<?> selectSysArticleById(@PathVariable Long id) {
        SysArticle sysArticle = sysArticleService.selectSysArticleById(id);

        return R.ok(sysArticle);
    }

    /**
     * 添加文章
     * */
    @PostMapping("/add")
    public R<?> addSysArticle(@RequestBody SysArticle sysArticle) {
        sysArticleService.addSysArticle(sysArticle);
        return R.ok("新增成功");
    }

    /**
     * 更新文章
     * */
    @PutMapping("/update")
    public R<?> updateSysArticle(@RequestBody SysArticle sysArticle) {
        sysArticleService.updateSysArticle(sysArticle);

        return R.ok("更新成功");
    }
}
