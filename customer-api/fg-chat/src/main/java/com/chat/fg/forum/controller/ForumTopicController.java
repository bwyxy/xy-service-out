package com.chat.fg.forum.controller;

import com.xy.common.core.domain.R;
import com.xy.system.service.forum.IForumTopicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

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
    public R<Object> hotTopics() {
        // TODO 后续实现热门话题业务
        return R.ok(new ArrayList<>());
    }
}
