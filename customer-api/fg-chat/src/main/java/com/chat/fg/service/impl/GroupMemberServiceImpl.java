package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.GroupMemberMapper;
import com.chat.fg.pojo.dto.GroupNicknameDTO;
import com.chat.fg.pojo.dto.MuteMemberDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.pojo.vo.GroupMemberVO;
import com.chat.fg.service.IGroupMemberService;
import com.chat.fg.service.IUserService;
import com.chat.fg.utils.SecurityUtils;
import com.chat.fg.wk.ChatMessage;
import com.xy.common.entity.User;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 群组成员 逻辑层
 * */
@Service
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements IGroupMemberService {

    @Autowired
    private ChatMessage chatMessage;

    @Autowired
    private IUserService userService;

    /**
     * 根据id查询群成员在群里的拓展信息
     * */
    @Override
    public GroupMember getGroupMemberByUid(Long uId, Long groupId) {
        if (uId == null || groupId == null) {
            throw new ServiceException("用户Id和群聊Id不能为空");
        }
        LambdaQueryWrapper<GroupMember> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(GroupMember::getGroupId, groupId)
                .eq(GroupMember::getUId, uId);

        return this.getOne(queryWrapper);
    }

    /**
     * 根据群聊 ID查询成员信息
     * */
    @Override
    public List<GroupMemberVO> selectGroupMemberByGroupId(String groupId) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(groupId);

        if (matcher.find()) {
            Long id = Long.valueOf(matcher.group());

            return baseMapper.selectGroupMemberByGroupId(id);
        }
        throw new ServiceException("无效群聊 ID");
    }

    /**
     * 批量添加成员
     * */
    @Override
    public void batchGroupMember(Long groupId, List<Long> memberIds) {

        LocalDateTime now = LocalDateTime.now();
        List<GroupMember> groupMembers = new ArrayList<>(memberIds.size());

        for (Long memberId : memberIds) {
            GroupMember groupMember = new GroupMember();

            groupMember.setGroupId(groupId);
            groupMember.setUId(memberId);
            groupMember.setRole(0);
            groupMember.setJoinedAt(now);

            groupMembers.add(groupMember);
        }

        saveBatch(groupMembers);
    }

    /**
     * 群成员禁言/解禁
     *
     * <p>仅群主/管理员可操作，不能对群主禁言；muteMinutes 为空或小于等于 0 时解除禁言。</p>
     * */
    @Override
    public void muteMember(MuteMemberDTO muteMemberDTO) {
        Long uId = SecurityUtils.getUId();

        GroupMember operator = getGroupMemberByUid(uId, muteMemberDTO.getGroupId());

        if (operator == null || operator.getRole() == GroupMemberRole.regular.getCode()) {
            throw new ServiceException("您没有权限禁言群成员");
        }

        GroupMember target = getGroupMemberByUid(muteMemberDTO.getUid(), muteMemberDTO.getGroupId());

        if (target == null) {
            throw new ServiceException("该成员不在群聊中");
        }
        if (target.getRole() == GroupMemberRole.owner.getCode()) {
            throw new ServiceException("不能对群主进行禁言");
        }

        LambdaUpdateWrapper<GroupMember> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(GroupMember::getId, target.getId());

        boolean unmute = muteMemberDTO.getMuteMinutes() == null || muteMemberDTO.getMuteMinutes() <= 0;

        if (unmute) {
            updateWrapper.set(GroupMember::getMuteEndTime, null);
        } else {
            updateWrapper.set(GroupMember::getMuteEndTime, LocalDateTime.now().plusMinutes(muteMemberDTO.getMuteMinutes()));
        }

        update(updateWrapper);

        // 向群频道播报禁言变更（无发送者的频道系统消息），群成员可实时感知
        String nickname = resolveNickname(muteMemberDTO.getUid());

        chatMessage.sendGroupMessage(
                muteMemberDTO.getGroupId().toString(),
                null,
                MessageType.groupMute,
                unmute
                        ? nickname + " 已被解除禁言"
                        : nickname + " 已被禁言 " + muteMemberDTO.getMuteMinutes() + " 分钟"
        );
    }

    /**
     * 查询用户昵称，查询不到时兜底展示
     * */
    private String resolveNickname(Long uId) {
        User user = userService.getById(uId);

        if (user == null || StringUtils.isEmpty(user.getNickname())) {
            return "用户" + uId;
        }

        return user.getNickname();
    }

    /**
     * 修改当前用户在群内的昵称
     * */
    @Override
    public void updateGroupNickname(GroupNicknameDTO groupNicknameDTO) {
        Long uId = SecurityUtils.getUId();

        GroupMember groupMember = getGroupMemberByUid(uId, groupNicknameDTO.getGroupId());

        if (groupMember == null) {
            throw new ServiceException("您不在该群聊中");
        }

        LambdaUpdateWrapper<GroupMember> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(GroupMember::getId, groupMember.getId())
                .set(GroupMember::getGroupNickname, groupNicknameDTO.getGroupNickname());

        update(updateWrapper);
    }
}
