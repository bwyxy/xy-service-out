package com.xy.blog.service;

import com.xy.common.entity.SysCategory;
import com.xy.common.vo.blog.CategoryVO;
import com.xy.common.vo.blog.ToolCategoryVO;

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

    /**
     * 查询分类关联工具列表
     * */
    public List<ToolCategoryVO> listCategoryWithTools();

    /**
     * 查询相册分类
     * */
    List<SysCategory> listAlbumCategory();

    /**
     * 查询工具分类
     * */
    List<SysCategory> listToolCategory();
}
