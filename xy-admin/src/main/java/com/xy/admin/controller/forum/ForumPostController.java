package com.xy.admin.controller.forum;

import com.xy.common.core.domain.R;
import com.xy.common.entity.forum.ForumPost;
import com.xy.system.service.forum.IForumPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛帖子
 * */
@RestController
@RequestMapping("/admin/forum")
@RequiredArgsConstructor
public class ForumPostController {

    @Autowired
    private IForumPostService forumPostService;

    /**
     * 获取帖子列表
     * */
    @PostMapping("/posts")
    public R<?> list(@RequestBody ForumPost post) {

        return R.ok(forumPostService.getForumPosts(post));
    }

    /**
     * 审核帖子
     * */
    @PostMapping("/posts/{id}/audit/{status}")
    public R<?> audit(@PathVariable Long id, @PathVariable Integer status) {
        String msgText = forumPostService.auditPost(id, status);

        return R.ok(msgText);
    }

    /**
     * 置顶或取消
     * */
    @PostMapping("/posts/{id}/top")
    public R<?> top(@PathVariable Long id) {
        forumPostService.topPost(id);

        return R.ok("操作成功");
    }

    /**
     * 加精或取消
     * */
    @PostMapping("/posts/{id}/essence")
    public R<?> essence(@PathVariable Long id) {
        forumPostService.essencePost(id);

        return R.ok("操作成功");
    }
}
