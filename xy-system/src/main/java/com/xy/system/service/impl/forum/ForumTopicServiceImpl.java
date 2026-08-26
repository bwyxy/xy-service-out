package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumTopic;
import com.xy.system.mapper.forum.ForumTopicMapper;
import com.xy.system.service.forum.IForumTopicService;
import org.springframework.stereotype.Service;

/**
 * 论坛话题 Service 实现
 */
@Service
public class ForumTopicServiceImpl extends ServiceImpl<ForumTopicMapper, ForumTopic>
        implements IForumTopicService {
}
