package com.chat.fg.service.forum;

import com.chat.fg.pojo.dto.forum.BoardDTO;
import com.xy.common.entity.forum.ForumBoard;

import java.util.List;

/**
 * 查询版块列表
 * */
public interface IBoardService {

    /**
     * 查询版块列表
     * */
    List<ForumBoard> selectBoards(BoardDTO boardDTO);
}
