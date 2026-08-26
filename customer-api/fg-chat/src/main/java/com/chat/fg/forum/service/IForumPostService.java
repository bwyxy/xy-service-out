package com.chat.fg.forum.service;

import com.chat.fg.forum.pojo.dto.CreatePostDTO;

import java.util.Map;

/**
 * 论坛帖子 服务接口
 */
public interface IForumPostService {

    /**
     * 发布帖子
     */
    Long create(Long currentUid, CreatePostDTO dto);

    /**
     * 帖子详情
     */
    Map<String, Object> detail(Long postId);
}
