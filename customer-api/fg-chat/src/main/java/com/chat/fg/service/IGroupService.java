package com.chat.fg.service;

import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.pojo.dto.EditGroupDTO;
import com.chat.fg.pojo.vo.GroupVO;
import com.xy.common.entity.chat.Group;

import java.util.List;

/**
 * 群聊 逻辑层
 * */
public interface IGroupService {

    /**
     * 查询群聊信息
     * */
    Group findById(String id);

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

    /**
     * 查询当前用户加入的群聊列表（不含已解散的群）
     * */
    List<GroupVO> listMyGroups();
}
