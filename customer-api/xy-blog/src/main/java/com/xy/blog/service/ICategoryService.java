package com.xy.blog.service;

import com.xy.common.entity.SysCategory;

import java.util.List;

public interface ICategoryService {

    /**
     * 获取全部分类列表
     * */
    public List<SysCategory> getCategoryAll(String keyword);
}
