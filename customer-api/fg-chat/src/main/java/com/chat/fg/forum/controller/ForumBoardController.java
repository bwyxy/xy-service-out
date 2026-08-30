package com.chat.fg.forum.controller;

import com.chat.fg.pojo.dto.forum.BoardDTO;
import com.chat.fg.service.forum.IBoardService;
import com.xy.common.core.domain.R;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.system.service.forum.IForumBoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 论坛板块 控制层
 */
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumBoardController {

    private final IForumBoardService forumBoardService;

    private final IBoardService boardService;

    /**
     * 查询板块树
     */
    @GetMapping("/board/tree")
    public R<List<ForumBoard>> tree(ForumBoard forumBoard) {
        return R.ok(forumBoardService.selectForumBoardTree(forumBoard));
    }

    /**
     * 查询板块列表
     */
    @PostMapping("/boards")
    public R<List<ForumBoard>> list(@RequestBody BoardDTO boardDTO) {
        return R.ok(boardService.selectBoards(boardDTO));
    }

    /**
     * 更新板块
     */
    @PutMapping("/board/{boardId}")
    public R<Void> update(@PathVariable Long boardId, @Valid @RequestBody ForumBoard forumBoard) {
        forumBoard.setBoardId(boardId);
        forumBoardService.updateForumBoard(forumBoard);
        return R.ok();
    }
}
