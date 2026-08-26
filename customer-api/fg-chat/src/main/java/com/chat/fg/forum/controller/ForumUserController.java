package com.chat.fg.forum.controller;

import com.chat.fg.forum.service.IForumUserService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 论坛用户 控制层
 */
@RestController
@RequestMapping("/api/forum/user")
@RequiredArgsConstructor
public class ForumUserController {

    private final IForumUserService forumUserService;

    /**
     * 用户论坛主页
     */
    @GetMapping("/{uId}/profile")
    public R<Map<String, Object>> profile(@PathVariable Long uId) {
        return R.ok(forumUserService.profile(uId));
    }
}
