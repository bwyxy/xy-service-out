package com.xy.system.service.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogPhoto;

public interface IBlogPhotoService {

    /**
     * 分页查询照片列表
     * */
    PageResult<BlogPhoto> getBlogPhotoByPage(BlogPhoto blogPhoto);

    /**
     * 根据id 查询照片信息
     * */
    BlogPhoto getBlogPhotoById(Long id);

    /**
     * 添加照片
     * */
    void addBlogPhoto(BlogPhoto blogPhoto);

    /**
     * 更新照片
     * */
    void updateBlogPhoto(BlogPhoto blogPhoto);

    /**
     * 删除照片
     * */
    void deleteBlogPhoto(Long id);
}
