package com.xy.admin.controller.forum;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.system.service.forum.IForumBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 论坛版块 控制层
 * */
@RestController
@RequestMapping("/admin/forum")
@RequiredArgsConstructor
public class ForumBoardController {

    @Autowired
    private IForumBoardService forumBoardService;

    /**
     * 查询论坛版块列表
     * */
    @PostMapping("/boards/list")
    public R<PageResult<ForumBoard>> getForumBoards(@RequestBody ForumBoard forumBoard) {

        return R.ok(forumBoardService.selectForumBoardList(forumBoard));
    }

    /**
     * 添加论坛版块
     * */
    @PostMapping("/boards")
    public R<String> addForumBoard(@RequestBody ForumBoard forumBoard) {
        forumBoardService.addForumBoard(forumBoard);

        return R.ok("success");
    }

    /**
     * 更新论坛版块
     * */
    @PutMapping("/boards")
    public R<String> updateForumBoard(@RequestBody ForumBoard forumBoard) {
        forumBoardService.updateForumBoard(forumBoard);

        return R.ok("success");
    }

    /**
     * 删除论坛版块
     * */
    @DeleteMapping("/boards/{boardId}")
    public R<String> deleteForumBoard(@PathVariable Long boardId) {
        forumBoardService.deleteForumBoard(boardId);

        return R.ok("success");
    }
}
