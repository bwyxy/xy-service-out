package com.chat.fg.forum.service;

import java.util.List;
import java.util.Map;

/**
 * 论坛板块 服务接口
 */
public interface IForumBoardService {

    /**
     * 查询板块树
     */
    List<Map<String, Object>> tree();

    /**
     * 板块详情
     */
    Map<String, Object> detail(Long boardId);
}
