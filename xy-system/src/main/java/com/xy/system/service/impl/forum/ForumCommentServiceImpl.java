package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumComment;
import com.xy.system.mapper.forum.ForumCommentMapper;
import com.xy.system.service.forum.IForumCommentService;
import org.springframework.stereotype.Service;

/**
 * 论坛评论 Service 实现
 */
@Service
public class ForumCommentServiceImpl extends ServiceImpl<ForumCommentMapper, ForumComment>
        implements IForumCommentService {
}
