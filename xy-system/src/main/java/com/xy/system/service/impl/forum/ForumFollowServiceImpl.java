package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumFollow;
import com.xy.system.mapper.forum.ForumFollowMapper;
import com.xy.system.service.forum.IForumFollowService;
import org.springframework.stereotype.Service;

/**
 * 论坛关注 Service 实现
 */
@Service
public class ForumFollowServiceImpl extends ServiceImpl<ForumFollowMapper, ForumFollow>
        implements IForumFollowService {
}
