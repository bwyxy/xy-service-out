package com.xy.system.service.forum;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.forum.ForumPost;

/**
 * 论坛帖子 Service 接口
 */
public interface IForumPostService extends IService<ForumPost> {

    /**
     * 查询论坛列表
     * */
    PageResult<ForumPost> getForumPosts(ForumPost forumPost);

    /**
     * 审核或拒绝
     * */
    String auditPost(Long postId, int status);
}
