package com.chat.fg.forum.controller;

import com.chat.fg.forum.pojo.dto.CreatePostDTO;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.R;
import com.xy.system.service.forum.IForumPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛帖子 控制层
 */
@RestController
@RequestMapping("/api/forum/post")
@RequiredArgsConstructor
public class ForumPostController {

    private final IForumPostService postService;

    /**
     * 发布帖子
     */
    @PostMapping
    public R<Long> create(@Valid @RequestBody CreatePostDTO dto) {
        Long currentUid = SecurityUtils.getUId();
        // TODO 后续实现帖子发布业务
        return R.ok(null);
    }

    /**
     * 帖子详情
     */
    @GetMapping("/{postId}")
    public R<Object> detail(@PathVariable Long postId) {
        // TODO 后续实现帖子详情业务
        return R.ok(null);
    }
}
