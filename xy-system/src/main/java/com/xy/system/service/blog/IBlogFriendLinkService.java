package com.xy.system.service.blog;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.blog.BlogFriendLink;

public interface IBlogFriendLinkService {

    /**
     * 分页查询友链列表
     * */
    PageResult<BlogFriendLink> selectBlogFriendListByPage(BlogFriendLink blogFriendLink);

    /**
     * 根据id 查询友链详情
     * */
    BlogFriendLink selectBlogFriendLinkById(Long id);

    /**
     * 添加友链
     * */
    void insertBlogFriendLink(BlogFriendLink blogFriendLink);

    /**
     * 编辑友链
     * */
    void updateBlogFriendLink(BlogFriendLink blogFriendLink);

    /**
     * 删除友链
     * */
    void deleteBlogFriendLinkById(Long id);
}
