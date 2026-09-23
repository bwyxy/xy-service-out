package com.xy.blog.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.blog.ArticleDTO;
import com.xy.common.entity.SysArticle;
import com.xy.common.vo.ArticleDetailsVO;
import com.xy.common.vo.blog.ArticleVO;

public interface IArticleService {

    /**
     * 查询文章列表
     * */
    public PageResult<ArticleVO> selectSysArticle(ArticleDTO articleDTO);

    /**
     * 根据文章 Id查询文章详情
     * */
    public SysArticle selectSysArticleById(Long id);

    /**
     * 根据文章slug 查询文章详情
     * */
    ArticleDetailsVO selectArticleDetailsBySlug(String slug);

    /**
     * 根据文章 slug查询文章详情
     * */
    public SysArticle selectSysArticleBySlug(String slug);
}
