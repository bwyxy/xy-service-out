package com.todo.system.service;

import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysArticle;

public interface ISysArticleService {

    /**
     * 分页查询文章列表
     * */
    public PageResult<SysArticle> selectSysArticleList(SysArticle sysArticle);

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
