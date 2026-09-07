package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.pojo.dto.EditGroupDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.service.IGroupService;
import com.chat.fg.utils.SecurityUtils;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.constant.Constants;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 群聊 逻辑层
 * */
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements IGroupService {

    @Autowired
    private GroupMemberServiceImpl groupMemberService;

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 查询群聊信息
     * */
    @Override
    public Group findById(Long id) {
        if (id == null) {
            throw new ServiceException("群聊Id不能为空");
        }
        LambdaQueryWrapper<Group> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(Group::getGroupId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 创建群聊
     */
    @Override
    public void createGroup(CreateGroupDTO createGroupDTO) {

        Long uId = SecurityUtils.getUId();
        LocalDateTime now = LocalDateTime.now();

        Group group = new Group();
        group.setName(createGroupDTO.getGroupName());
        group.setOwnerUid(createGroupDTO.getUid());
        group.setCreateTime(now);
        group.setUpdateTime(now);
        group.setCreateBy(uId);
        group.setUpdateBy(uId);

        boolean success = save(group);

        if (success) {
            Long groupId = group.getGroupId();
            // 添加群主 Id
            addGroupOwner(groupId, createGroupDTO.getUid());
            // 添加群成员
            groupMemberService.batchGroupMember(groupId, createGroupDTO.getMemberIds());

            sendNewMemberNotice(groupId, createGroupDTO.getRemark());
        } else {
            throw new ServiceException("创建群聊失败");
        }
    }

    /**
     * 编辑群资料
     * */
    @Override
    public void editGroup(EditGroupDTO editGroupDTO) {
        Long uId = SecurityUtils.getUId();
        LocalDateTime now = LocalDateTime.now();

        GroupMember groupMember = groupMemberService.getGroupMemberByUid(uId, editGroupDTO.getGroupId());

        if (groupMember == null) {
            throw new ServiceException("查询该成员在群里信息为空");
        }
        if (groupMember.getRole() == GroupMemberRole.regular.getCode()) {
            throw new ServiceException("您没有权限修改群聊信息");
        }

        LambdaUpdateWrapper<Group> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Group::getGroupId, editGroupDTO.getGroupId());

        if (editGroupDTO.getName() != null && !StringUtils.isEmpty(editGroupDTO.getName())) {
            updateWrapper.set(Group::getName, editGroupDTO.getName());
        }
        if (editGroupDTO.getAvatar() != null && !StringUtils.isEmpty(editGroupDTO.getAvatar())) {
            updateWrapper.set(Group::getAvatar, editGroupDTO.getAvatar());
        }
        if (editGroupDTO.getIsBan() != null) {
            updateWrapper.set(Group::getIsBan, editGroupDTO.getIsBan());
        }
        if (editGroupDTO.getHistoryVisible() != null) {
            updateWrapper.set(Group::getHistoryVisible, editGroupDTO.getHistoryVisible());
        }
        updateWrapper.set(Group::getUpdateBy, uId);
        updateWrapper.set(Group::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 解散群聊
     * */
    @Override
    public void dissolveGroup(Long groupId) {

        if (groupId == null) {
            throw new ServiceException("群聊Id不能为空");
        }

        Long uId = SecurityUtils.getUId();
        LocalDateTime now = LocalDateTime.now();

        GroupMember groupMember = groupMemberService.getGroupMemberByUid(uId, groupId);

        if (groupMember.getRole() != GroupMemberRole.owner.getCode()) {
            throw new ServiceException("只有群主才能解散群聊");
        }
        LambdaUpdateWrapper<Group> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(Group::getGroupId, groupId)
                .set(Group::getDisband, 1)
                .set(Group::getUpdateTime, now);

        update(updateWrapper);
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

    /**
     * 发送新成员通知
     * */
    private void sendNewMemberNotice(Long groupId, String memberName) {
        MessageSendRequest messageSendRequest = new MessageSendRequest();

        messageSendRequest.setChannelId(Constants.GROUP_ID_PREFIX + groupId);
        messageSendRequest.setChannelType(ChannelType.GROUP_CHANNEL.getCode());

        Map<String, Object> map = new HashMap<>();

        map.put("type", MessageType.newMember.getCode());
        map.put("content", memberName + "加入群聊");
        String jsonStr = JSON.toJSONString(map);

        String base64Payload = Base64.getEncoder().encodeToString(jsonStr.getBytes(StandardCharsets.UTF_8));

        messageSendRequest.setPayload(base64Payload);

        MessageSendRequest.MessageHeader messageHeader = new MessageSendRequest.MessageHeader();

        messageHeader.setRedDot(1);

        messageSendRequest.setHeader(messageHeader);

        wuKongClient.sendMessage(messageSendRequest);
    }
}
