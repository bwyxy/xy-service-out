package com.xy.system.service.forum;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.forum.ForumBoard;

/**
 * 论坛板块 Service 接口
 */
public interface IForumBoardService extends IService<ForumBoard> {

    /**
     * 查询版块列表
     * */
    PageResult<ForumBoard> selectForumBoardList(ForumBoard forumBoard);

    /**
     * 添加版块
     * */
    void addForumBoard(ForumBoard forumBoard);

    /**
     * 更新版块
     * */
    void updateForumBoard(ForumBoard forumBoard);

    /**
     * 删除版块
     * */
    void deleteForumBoard(Long boardId);
}
