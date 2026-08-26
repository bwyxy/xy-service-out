package com.chat.fg.forum.service;

import java.util.List;
import java.util.Map;

/**
 * 论坛评论 服务接口
 */
public interface IForumCommentService {

    /**
     * 根据帖子查询评论列表
     */
    List<Map<String, Object>> listByPost(Long postId);
}
