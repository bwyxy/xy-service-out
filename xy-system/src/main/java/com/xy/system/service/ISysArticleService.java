package com.xy.system.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysArticle;

import java.util.List;

public interface ISysArticleService {

    /**
     * 分页查询文章列表
     * */
    public PageResult<SysArticle> selectSysArticleList(SysArticle sysArticle);

    /**
     * 查询推荐文章
     * */
    List<SysArticle> selectArticleList();

    /**
     * 根据文章 Id获取文章信息
     * */
    public SysArticle selectSysArticleById(Long id);

    /**
     * 新增文章
     * */
    public void addSysArticle(SysArticle sysArticle);

    /**
     * 更新文章
     * */
    public void updateSysArticle(SysArticle sysArticle);
}
