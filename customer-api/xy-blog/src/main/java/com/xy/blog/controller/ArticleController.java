package com.xy.blog.controller;

import com.xy.blog.service.IArticleService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.blog.ArticleDTO;
import com.xy.common.entity.SysArticle;
import com.xy.common.vo.blog.ArticleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/article")
@RequiredArgsConstructor
public class ArticleController {

    @Autowired
    private IArticleService articleService;

    /**
     * 获取文章列表
     * */
    @Anonymous
    @PostMapping("/list")
    public R<?> list(@RequestBody ArticleDTO articleDTO) {

        PageResult<ArticleVO> pageResult = articleService.selectSysArticle(articleDTO);

        return R.ok(pageResult);
    }

    /**
     * 根据文章 Id查询文章信息
     * */
    @GetMapping("/{id}")
    public R<?> getOne(@PathVariable Long id) {

        SysArticle sysArticle = articleService.selectSysArticleById(id);

        return R.ok(sysArticle);
    }

    /**
     * 根据文章 slug查询文章信息
     * */
    @GetMapping("/info/{slug}")
    public R<?> getArticleBySlug(@PathVariable String slug) {
        SysArticle sysArticle = articleService.selectSysArticleBySlug(slug);

        return R.ok(sysArticle);
    }
}
