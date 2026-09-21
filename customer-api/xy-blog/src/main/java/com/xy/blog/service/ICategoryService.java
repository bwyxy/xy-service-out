package com.xy.blog.service;

import com.xy.common.entity.SysCategory;
import com.xy.common.vo.blog.CategoryVO;

import java.util.List;

public interface ICategoryService {

    /**
     * 获取全部分类列表
     * */
    public List<SysCategory> getCategoryAll(String keyword);

    /**
     * 查询分类关联文章列表
     * */
    public List<CategoryVO> listCategoryWithArticles();
}
