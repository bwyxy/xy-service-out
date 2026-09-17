package com.xy.system.service.blog;

import com.xy.common.entity.blog.BlogMenuConfig;

import java.util.List;

public interface IBlogMenuConfigService {

    /**
     * 根据菜单Id获取菜单配置列表
     * */
    List<BlogMenuConfig> selectBlogMenuConfigByMenuId(Long menuId);

    /**
     * 获取菜单配置信息
     * */
    BlogMenuConfig selectBlogMenuConfigById(Long id);

    /**
     * 添加菜单配置
     * */
    void addBlogMenuConfig(BlogMenuConfig blogMenuConfig);

    /**
     * 编辑菜单配置
     * */
    void editBlogMenuConfig(BlogMenuConfig blogMenuConfig);

    /**
     * 根据配置Id删除菜单配置
     * */
    void deleteBlogMenuConfig(Long id);
}
