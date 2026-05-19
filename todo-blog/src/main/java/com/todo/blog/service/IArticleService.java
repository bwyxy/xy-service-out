package com.todo.blog.service;

import com.todo.blog.pojo.dto.ArticleDTO;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysArticle;

import java.util.List;

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
