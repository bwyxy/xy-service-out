package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumPost;
import com.xy.system.mapper.forum.ForumPostMapper;
import com.xy.system.service.forum.IForumPostService;
import org.springframework.stereotype.Service;

/**
 * 论坛帖子 Service 实现
 */
@Service
public class ForumPostServiceImpl extends ServiceImpl<ForumPostMapper, ForumPost>
        implements IForumPostService {
}
