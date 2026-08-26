package com.chat.fg.forum.service.impl;

import com.chat.fg.forum.service.IForumBoardService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 论坛板块 服务实现
 */
@Service
public class ForumBoardServiceImpl implements IForumBoardService {

    @Override
    public List<Map<String, Object>> tree() {
        // TODO 后续实现板块树查询
        return new ArrayList<>();
    }

    @Override
    public Map<String, Object> detail(Long boardId) {
        // TODO 后续实现板块详情
        return new HashMap<>();
    }
}
