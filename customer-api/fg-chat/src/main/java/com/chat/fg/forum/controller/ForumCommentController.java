package com.chat.fg.forum.controller;

import com.chat.fg.forum.service.IForumCommentService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 论坛评论 控制层
 */
@RestController
@RequestMapping("/api/forum/comment")
@RequiredArgsConstructor
public class ForumCommentController {

    private final IForumCommentService commentService;

    /**
     * 评论列表
     */
    @GetMapping("/post/{postId}")
    public R<List<Map<String, Object>>> listByPost(@PathVariable Long postId) {
        return R.ok(commentService.listByPost(postId));
    }
}
