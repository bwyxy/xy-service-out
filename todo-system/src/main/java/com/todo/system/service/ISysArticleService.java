package com.todo.system.service;

import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysArticle;

public interface ISysArticleService {

    /**
     * 分页查询文章列表
     * */
    public PageResult<SysArticle> selectSysArticleList(SysArticle sysArticle);
}
