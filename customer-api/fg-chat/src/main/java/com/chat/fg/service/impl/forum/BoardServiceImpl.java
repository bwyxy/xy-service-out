package com.chat.fg.service.impl.forum;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.dto.forum.BoardDTO;
import com.chat.fg.service.forum.IBoardService;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.system.service.impl.forum.ForumBoardServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BoardServiceImpl implements IBoardService {

    @Autowired
    private ForumBoardServiceImpl forumBoardService;

    /**
     * 查询版块列表
     * */
    public List<ForumBoard> selectBoards(BoardDTO boardDTO) {

        LambdaQueryWrapper<ForumBoard> queryWrapper = new LambdaQueryWrapper<>();

        Long parentId = boardDTO.getParentId() == null ? 0L : boardDTO.getParentId();
        queryWrapper.eq(ForumBoard::getParentId, parentId)
                .orderByDesc(ForumBoard::getSortOrder);

        return forumBoardService.list(queryWrapper);
    }

}
