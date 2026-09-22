package com.xy.system.service.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogTool;

public interface IBlogToolService {

    /**
     * 分页查询工具列表
     * */
    PageResult<BlogTool> getBlogToolList(BlogTool blogTool);

    /**
     * 根据id 查询工具信息
     * */
    BlogTool getBlogToolById(Long id);

    /**
     * 添加工具
     * */
    void addBlogTool(BlogTool blogTool);

    /**
     * 更新工具
     * */
    void updateBlogTool(BlogTool blogTool);

    /**
     * 删除工具
     * */
    void deleteBlogTool(Long id);
}
