package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumPostTopic;
import com.xy.system.mapper.forum.ForumPostTopicMapper;
import com.xy.system.service.forum.IForumPostTopicService;
import org.springframework.stereotype.Service;

/**
 * 帖子话题关联 Service 实现
 */
@Service
public class ForumPostTopicServiceImpl extends ServiceImpl<ForumPostTopicMapper, ForumPostTopic>
        implements IForumPostTopicService {
}
