package com.chat.fg.forum.controller;

import com.chat.fg.pojo.dto.forum.BoardDTO;
import com.chat.fg.service.forum.IBoardService;
import com.xy.common.core.domain.R;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.system.service.forum.IForumBoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 论坛板块 控制层
 */
@RestController
@RequestMapping("/api/forum")
@RequiredArgsConstructor
public class ForumBoardController {

    @Autowired
    private IBoardService boardService;

    /**
     * 查询板块列表
     */
    @PostMapping("/boards")
    public R<List<ForumBoard>> list(@RequestBody BoardDTO boardDTO) {

        return R.ok(boardService.selectBoards(boardDTO));
    }
}
