package com.chat.fg.service;

import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.pojo.dto.EditGroupDTO;
import com.xy.common.entity.chat.Group;

/**
 * 群聊 逻辑层
 * */
public interface IGroupService {

    /**
     * 查询群聊信息
     * */
    Group findById(Long id);

    /**
     * 创建群聊
     */
    void createGroup(CreateGroupDTO createGroupDTO);

    /**
     * 编辑群资料
     * */
    void editGroup(EditGroupDTO editGroupDTO);

    /**
     * 解散群聊
     * */
    void dissolveGroup(Long groupId);
}
