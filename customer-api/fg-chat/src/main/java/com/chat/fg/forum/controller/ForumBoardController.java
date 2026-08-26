package com.chat.fg.forum.controller;

import com.chat.fg.forum.service.IForumBoardService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 论坛板块 控制层
 */
@RestController
@RequestMapping("/api/forum/board")
@RequiredArgsConstructor
public class ForumBoardController {

    private final IForumBoardService boardService;

    /**
     * 查询板块树
     */
    @GetMapping("/tree")
    public R<List<Map<String, Object>>> tree() {
        return R.ok(boardService.tree());
    }

    /**
     * 板块详情
     */
    @GetMapping("/{boardId}")
    public R<Map<String, Object>> detail(@PathVariable Long boardId) {
        return R.ok(boardService.detail(boardId));
    }
}
