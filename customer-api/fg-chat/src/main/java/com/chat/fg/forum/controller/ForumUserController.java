package com.chat.fg.forum.controller;

import com.xy.common.core.domain.R;
import com.xy.system.service.forum.IForumUserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 论坛用户 控制层
 */
@RestController
@RequestMapping("/api/forum/user")
@RequiredArgsConstructor
public class ForumUserController {

    private final IForumUserProfileService forumUserProfileService;

    /**
     * 用户论坛主页
     */
    @GetMapping("/{uId}/profile")
    public R<Object> profile(@PathVariable Long uId) {
        // TODO 后续实现用户论坛主页业务
        return R.ok(null);
    }
}
