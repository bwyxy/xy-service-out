package com.chat.fg.forum.controller;

import com.chat.fg.forum.pojo.dto.CreatePostDTO;
import com.chat.fg.forum.service.IForumPostService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

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
        return R.ok(postService.create(currentUid, dto));
    }

    /**
     * 帖子详情
     */
    @GetMapping("/{postId}")
    public R<Map<String, Object>> detail(@PathVariable Long postId) {
        return R.ok(postService.detail(postId));
    }
}
