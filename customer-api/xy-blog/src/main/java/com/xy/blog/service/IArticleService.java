package com.xy.blog.service;

import com.xy.blog.pojo.dto.ArticleDTO;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysArticle;

public interface IArticleService {

    /**
     * 查询文章列表
     * */
    public PageResult<SysArticle> selectSysArticle(ArticleDTO articleDTO);

    /**
     * 根据文章 Id查询文章详情
     * */
    public SysArticle selectSysArticleById(Long id);

    /**
     * 根据文章 slug查询文章详情
     * */
    public SysArticle selectSysArticleBySlug(String slug);
}
