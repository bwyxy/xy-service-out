package com.chat.fg.forum.controller;

import com.chat.fg.forum.service.IForumTopicService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 论坛话题 控制层
 */
@RestController
@RequestMapping("/api/forum/topic")
@RequiredArgsConstructor
public class ForumTopicController {

    private final IForumTopicService topicService;

    /**
     * 热门话题
     */
    @GetMapping("/hot")
    public R<List<Map<String, Object>>> hotTopics() {
        return R.ok(topicService.hotTopics());
    }
}
