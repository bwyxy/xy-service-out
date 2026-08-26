package com.chat.fg.forum.service.impl;

import com.chat.fg.forum.service.IForumCommentService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 论坛评论 服务实现
 */
@Service
public class ForumCommentServiceImpl implements IForumCommentService {

    @Override
    public List<Map<String, Object>> listByPost(Long postId) {
        // TODO 后续实现评论列表查询
        return new ArrayList<>();
    }
}
