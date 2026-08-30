package com.chat.fg.forum.controller;

import com.chat.fg.forum.pojo.dto.CreatePostDTO;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.forum.ForumPost;
import com.xy.system.service.forum.IForumPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛帖子 控制层
 */
@RestController
@RequestMapping("/forum/posts")
@RequiredArgsConstructor
public class ForumPostController {

    private final IForumPostService postService;

    /**
     * 发布帖子
     */
    @PostMapping("/create")
    public R<Long> create(@Valid @RequestBody CreatePostDTO dto) {
        Long currentUid = SecurityUtils.getUId();
        // TODO 后续实现帖子发布业务
        return R.ok(null);
    }

    /**
     * 查询帖子列表
     */
    @PostMapping
    public R<PageResult<ForumPost>> list(@RequestBody ForumPost forumPost) {
        // 客户端只查询已发布帖子
        if (forumPost.getStatus() == null) {
            forumPost.setStatus(2);
        }
        return R.ok(postService.getForumPosts(forumPost));
    }

    /**
     * 帖子详情
     */
    @GetMapping("/{postId}")
    public R<ForumPost> detail(@PathVariable Long postId) {
        Long currentUid = SecurityUtils.getUId();
        ForumPost post = postService.getForumPostDetail(postId);
        // 统计浏览量
        postService.incrementViewCount(postId, currentUid);
        return R.ok(post);
    }

    /**
     * 点赞 / 取消点赞
     */
    @PostMapping("/{postId}/like")
    public R<Boolean> like(@PathVariable Long postId) {
        Long currentUid = SecurityUtils.getUId();
        return R.ok(postService.likePost(postId, currentUid));
    }

    /**
     * 查询当前用户是否已点赞
     */
    @GetMapping("/{postId}/like")
    public R<Boolean> hasLiked(@PathVariable Long postId) {
        Long currentUid = SecurityUtils.getUId();
        return R.ok(postService.hasLiked(postId, currentUid));
    }
}
