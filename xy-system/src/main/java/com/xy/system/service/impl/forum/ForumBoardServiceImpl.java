package com.xy.system.service.impl.forum;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.forum.ForumBoard;
import com.xy.system.mapper.forum.ForumBoardMapper;
import com.xy.system.service.forum.IForumBoardService;
import org.springframework.stereotype.Service;

/**
 * 论坛板块 Service 实现
 */
@Service
public class ForumBoardServiceImpl extends ServiceImpl<ForumBoardMapper, ForumBoard>
        implements IForumBoardService {
}
