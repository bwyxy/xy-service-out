package com.chat.fg.forum.service.impl;

import com.chat.fg.forum.service.IForumTopicService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 论坛话题 服务实现
 */
@Service
public class ForumTopicServiceImpl implements IForumTopicService {

    @Override
    public List<Map<String, Object>> hotTopics() {
        // TODO 后续实现热门话题查询
        return new ArrayList<>();
    }
}
