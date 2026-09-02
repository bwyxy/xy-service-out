package com.chat.fg.service;

import com.chat.fg.pojo.dto.CreateGroupDTO;

/**
 * 群聊 逻辑层
 * */
public interface IGroupService {

    /**
     * 创建群聊
     */
    void createGroup(CreateGroupDTO createGroupDTO);
}
