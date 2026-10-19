package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.pojo.dto.CreateGroupDTO;
import com.chat.fg.pojo.dto.EditGroupDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.pojo.vo.GroupVO;
import com.chat.fg.service.IGroupService;
import com.chat.fg.utils.SecurityUtils;
import com.chat.fg.wk.ChatMessage;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.Channel;
import com.xy.common.constant.Constants;
import com.xy.common.entity.SysImUser;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.GroupMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 群聊 逻辑层
 * */
@Service
public class GroupServiceImpl extends ServiceImpl<GroupMapper, Group> implements IGroupService {

    @Autowired
    private GroupMemberServiceImpl groupMemberService;

    @Autowired
    private ChatMessage chatMessage;

    @Autowired
    private WuKongClient wuKongClient;

    /**
     * 查询群聊信息
     * */
    @Override
    public Group findById(String id) {
        if (id == null) {
            throw new ServiceException("群聊Id不能为空");
        }
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(id);

        if (matcher.find()) {
            LambdaQueryWrapper<Group> queryWrapper = new LambdaQueryWrapper<>();

            queryWrapper.eq(Group::getGroupId, matcher.group());

            return this.getOne(queryWrapper);
        }

        throw new ServiceException("无效群聊ID");
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

            List<String> memberIds = new ArrayList<>();

            memberIds.add(createGroupDTO.getUid().toString());
            for (Long memberId : createGroupDTO.getMemberIds()) {
                memberIds.add(memberId.toString());
            }

            // 添加群聊频道
            Channel channel = new Channel();
            channel.setChannelId(Constants.GROUP_ID_PREFIX + groupId);
            channel.setSubscribers(memberIds);

            wuKongClient.saveChannel(channel);
            // 向频道发送消息
            chatMessage.sendGroupMessage(
                    groupId.toString(),
                    memberIds,
                    createGroupDTO.getRemark() + "加入群聊"
            );
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

        // 全员禁言变更时向群频道播报（无发送者的频道系统消息）
        if (editGroupDTO.getIsBan() != null) {
            chatMessage.sendGroupMessage(
                    editGroupDTO.getGroupId().toString(),
                    null,
                    MessageType.groupMute,
                    editGroupDTO.getIsBan() == 1 ? "群主已开启全员禁言" : "群主已关闭全员禁言"
            );
        }
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
        // 删除该频道
        Channel channel = new Channel();
        channel.setChannelId(Constants.GROUP_ID_PREFIX + groupId);
        channel.setChannelType(2);

        wuKongClient.deleteChannel(channel);
    }

    /**
     * 查询当前用户加入的群聊列表
     * */
    @Override
    public List<GroupVO> listMyGroups() {
        Long uId = SecurityUtils.getUId();

        List<GroupMember> myMembers = groupMemberService.list(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getUId, uId));

        if (myMembers.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Integer> roleMap = new HashMap<>(myMembers.size());
        for (GroupMember member : myMembers) {
            roleMap.putIfAbsent(member.getGroupId(), member.getRole());
        }

        // 排除已解散的群聊
        List<Group> groups = this.list(new LambdaQueryWrapper<Group>()
                .in(Group::getGroupId, roleMap.keySet())
                .ne(Group::getDisband, 1));

        if (groups.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> groupIds = groups.stream().map(Group::getGroupId).collect(Collectors.toList());

        Map<Long, Long> memberCountMap = groupMemberService.list(
                        new LambdaQueryWrapper<GroupMember>().in(GroupMember::getGroupId, groupIds))
                .stream()
                .collect(Collectors.groupingBy(GroupMember::getGroupId, Collectors.counting()));

        List<GroupVO> groupList = new ArrayList<>(groups.size());

        for (Group group : groups) {
            GroupVO groupVO = new GroupVO();

            groupVO.setGroupId(group.getGroupId());
            groupVO.setName(group.getName());
            groupVO.setAvatar(group.getAvatar());
            groupVO.setRole(roleMap.get(group.getGroupId()));
            groupVO.setMemberCount(memberCountMap.getOrDefault(group.getGroupId(), 0L).intValue());

            groupList.add(groupVO);
        }

        // 新建的群聊排在前面
        groupList.sort(Comparator.comparing(GroupVO::getGroupId).reversed());

        return groupList;
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
