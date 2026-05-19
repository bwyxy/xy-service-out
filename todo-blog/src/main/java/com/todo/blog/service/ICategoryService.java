package com.todo.blog.service;

import com.todo.common.entity.SysCategory;

import java.util.List;

public interface ICategoryService {

    /**
     * 获取全部分类列表
     * */
    public List<SysCategory> getCategoryAll(String keyword);
}
