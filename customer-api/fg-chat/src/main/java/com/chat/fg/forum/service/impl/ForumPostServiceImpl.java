package com.chat.fg.forum.service.impl;

import com.chat.fg.forum.pojo.dto.CreatePostDTO;
import com.chat.fg.forum.service.IForumPostService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 论坛帖子 服务实现
 */
@Service
public class ForumPostServiceImpl implements IForumPostService {

    @Override
    public Long create(Long currentUid, CreatePostDTO dto) {
        // TODO 后续实现帖子发布
        return null;
    }

    @Override
    public Map<String, Object> detail(Long postId) {
        // TODO 后续实现帖子详情
        return new HashMap<>();
    }
}
