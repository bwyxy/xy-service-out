package com.xy.system.service.forum;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.forum.ForumPost;

/**
 * 论坛帖子 Service 接口
 */
public interface IForumPostService extends IService<ForumPost> {

    /**
     * 查询帖子列表（支持分页、排序、条件筛选）
     * */
    PageResult<ForumPost> getForumPosts(ForumPost forumPost);

    /**
     * 查询帖子详情
     * */
    ForumPost getForumPostDetail(Long postId);

    /**
     * 点赞 / 取消点赞
     *
     * @return true-点赞 false-取消点赞
     * */
    boolean likePost(Long postId, Long uId);

    /**
     * 查询用户是否已点赞
     * */
    boolean hasLiked(Long postId, Long uId);

    /**
     * 增加帖子浏览量（带防重复）
     * */
    void incrementViewCount(Long postId, Long uId);

    /**
     * 审核通过或拒绝
     * */
    String auditPost(Long postId, int status);

    /**
     * 帖子置顶或取消
     * */
    void topPost(Long postId);

    /**
     * 帖子加精或取消
     * */
    void essencePost(Long postId);
}
