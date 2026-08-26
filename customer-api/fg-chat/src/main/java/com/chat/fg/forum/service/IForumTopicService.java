package com.chat.fg.forum.service;

import java.util.List;
import java.util.Map;

/**
 * 论坛话题 服务接口
 */
public interface IForumTopicService {

    /**
     * 热门话题
     */
    List<Map<String, Object>> hotTopics();
}
