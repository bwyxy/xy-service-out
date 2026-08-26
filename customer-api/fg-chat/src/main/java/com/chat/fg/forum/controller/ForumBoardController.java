package com.chat.fg.forum.controller;

import com.xy.common.core.domain.R;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.system.service.forum.IForumBoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛板块 控制层
 */
@RestController
@RequestMapping("/api/forum/board")
@RequiredArgsConstructor
public class ForumBoardController {

    private final IForumBoardService boardService;

    /**
     * 更新板块
     */
    @PutMapping("/{boardId}")
    public R<Void> update(@PathVariable Long boardId, @Valid @RequestBody ForumBoard forumBoard) {
        forumBoard.setBoardId(boardId);
        boardService.updateForumBoard(forumBoard);
        return R.ok();
    }
}
