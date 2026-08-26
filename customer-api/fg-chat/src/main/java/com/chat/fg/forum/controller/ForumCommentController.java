package com.chat.fg.forum.controller;

import com.xy.common.core.domain.R;
import com.xy.system.service.forum.IForumCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

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
    public R<Object> listByPost(@PathVariable Long postId) {
        // TODO 后续实现评论列表业务
        return R.ok(new ArrayList<>());
    }
}
