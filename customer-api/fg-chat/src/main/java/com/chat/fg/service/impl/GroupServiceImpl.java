package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.service.IGroupService;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.exception.ServiceException;
import com.xy.system.mapper.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 群聊 逻辑层
 * */
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements IGroupService {

    @Autowired
    private GroupMemberServiceImpl groupMemberService;

    /**
     * 创建群聊
     */
    @Override
    public void createGroup(CreateGroupDTO createGroupDTO) {

        Group group = new Group();
        group.setName(createGroupDTO.getGroupName());
        group.setOwnerUid(createGroupDTO.getUId());

        boolean success = save(group);

        if (success) {
            Long groupId = group.getGroupId();
            // 添加群主 Id
            addGroupOwner(groupId, createGroupDTO.getUId());
            // 添加群成员
            groupMemberService.batchGroupMember(groupId, createGroupDTO.getMemberIds());
        } else {
            throw new ServiceException("创建群聊失败");
        }
    }

    /**
     * 添加群主成员
     * */
    private void addGroupOwner(Long groupId, Long ownerId) {
        LocalDateTime now = LocalDateTime.now();

        GroupMember groupMember = new GroupMember();
        groupMember.setGroupId(groupId);
        groupMember.setUId(ownerId);
        groupMember.setRole(2);
        groupMember.setJoinedAt(now);

        groupMemberService.save(groupMember);
    }
}
