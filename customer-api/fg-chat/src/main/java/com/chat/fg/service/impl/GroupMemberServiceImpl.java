package com.chat.fg.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.fg.mapper.GroupMemberMapper;
import com.chat.fg.pojo.dto.AddGroupMemberDTO;
import com.chat.fg.pojo.dto.GroupNicknameDTO;
import com.chat.fg.pojo.dto.MuteMemberDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.pojo.vo.GroupMemberVO;
import com.chat.fg.service.IGroupMemberService;
import com.chat.fg.service.IUserService;
import com.chat.fg.utils.DistributedLockHelper;
import com.chat.fg.utils.SecurityUtils;
import com.chat.fg.wk.ChatMessage;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.AddSubscriber;
import com.xy.common.client.dto.request.BaseChannel;
import com.xy.common.constant.Constants;
import com.xy.common.entity.User;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.ChannelIdUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.GroupMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 群组成员 逻辑层
 * */
@Slf4j
@Service
public class GroupMemberServiceImpl extends ServiceImpl<GroupMemberMapper, GroupMember> implements IGroupMemberService {

    /** 群成员变更的分布式锁 Key 前缀 */
    private static final String GROUP_MEMBER_LOCK_PREFIX = "group:member:lock:";

    /** 抢锁最大等待时间（秒） */
    private static final long LOCK_WAIT_SECONDS = 3L;

    /** 锁租约时间（秒） */
    private static final long LOCK_LEASE_SECONDS = 15L;

    @Autowired
    private ChatMessage chatMessage;

    @Autowired
    private IUserService userService;

    @Autowired
    private WuKongClient wuKongClient;

    @Autowired
    private GroupMapper groupMapper;

    @Autowired
    private DistributedLockHelper lockHelper;

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
        Long id = ChannelIdUtils.parseGroupId(groupId);

        if (id == null) {
            throw new ServiceException("无效群聊 ID");
        }

        return baseMapper.selectGroupMemberByGroupId(id);
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
     * 添加群成员（邀请好友入群）
     *
     * <p>仅群内成员可邀请；已在群内的成员会被自动过滤，并同步悟空 IM 频道订阅者。</p>
     *
     * <p>「查重 -> 校验名额 -> 插入」属于先查后写，并发邀请可能突破人数上限或写入重复成员，
     * 因此用群维度的分布式锁收敛并发窗口，最终一致性由 t_group_member(u_id, group_id) 唯一索引兜底。</p>
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addGroupMembers(AddGroupMemberDTO addGroupMemberDTO) {
        Long groupId = addGroupMemberDTO.getGroupId();

        lockHelper.tryLock(
                GROUP_MEMBER_LOCK_PREFIX + groupId,
                LOCK_WAIT_SECONDS,
                LOCK_LEASE_SECONDS,
                () -> doAddGroupMembers(addGroupMemberDTO),
                "群成员操作过于频繁，请稍后重试"
        );
    }

    private void doAddGroupMembers(AddGroupMemberDTO addGroupMemberDTO) {
        Long uId = SecurityUtils.getUId();
        Long groupId = addGroupMemberDTO.getGroupId();

        GroupMember operator = getGroupMemberByUid(uId, groupId);

        if (operator == null) {
            throw new ServiceException("您不在该群聊中，无法添加群成员");
        }

        Group group = groupMapper.selectById(groupId);

        if (group == null || Objects.equals(group.getDisband(), 1)) {
            throw new ServiceException("群聊不存在或已解散");
        }

        List<GroupMember> existMembers = this.list(
                new LambdaQueryWrapper<GroupMember>().eq(GroupMember::getGroupId, groupId));

        Set<Long> existUids = existMembers.stream().map(GroupMember::getUId).collect(Collectors.toSet());

        // 去掉重复提交和已在群内的成员
        List<Long> newMemberIds = addGroupMemberDTO.getMemberIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .filter(memberId -> !existUids.contains(memberId))
                .collect(Collectors.toList());

        if (newMemberIds.isEmpty()) {
            throw new ServiceException("所选成员均已在群聊中");
        }
        if (existMembers.size() + newMemberIds.size() > group.getMaxMember()) {
            throw new ServiceException("群成员人数已达上限");
        }

        try {
            batchGroupMember(groupId, newMemberIds);
        } catch (DuplicateKeyException e) {
            // 并发下唯一索引兜底：说明已有其它请求把同一批成员写入
            log.warn("并发添加群成员触发唯一索引冲突, groupId={}, memberIds={}", groupId, newMemberIds, e);
            throw new ServiceException("部分成员已在群聊中，请刷新后重试");
        }

        // 同步悟空 IM 频道订阅者，新成员才能收到该群消息
        AddSubscriber addSubscriber = new AddSubscriber();
        addSubscriber.setChannelId(Constants.GROUP_ID_PREFIX + groupId);
        addSubscriber.setSubscribers(newMemberIds.stream().map(String::valueOf).collect(Collectors.toList()));

        wuKongClient.addSubscribers(addSubscriber);

        // 向群频道播报入群通知
        String names = newMemberIds.stream().map(this::resolveNickname).collect(Collectors.joining("、"));

        chatMessage.sendGroupMessage(
                groupId.toString(),
                MessageType.groupMemberChange,
                names + " 加入了群聊"
        );
    }

    /**
     * 移除群成员（移出群聊）
     *
     * <p>仅群主/管理员可操作；群主不可被移出，管理员之间不能互相移出。</p>
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeGroupMember(Long groupId, Long uid) {
        Long uId = SecurityUtils.getUId();

        GroupMember operator = getGroupMemberByUid(uId, groupId);

        if (operator == null || operator.getRole() == GroupMemberRole.regular.getCode()) {
            throw new ServiceException("您没有权限移除群成员");
        }
        if (uId.equals(uid)) {
            throw new ServiceException("不能将自己移出群聊");
        }

        GroupMember target = getGroupMemberByUid(uid, groupId);

        if (target == null) {
            throw new ServiceException("该成员不在群聊中");
        }
        if (target.getRole() == GroupMemberRole.owner.getCode()) {
            throw new ServiceException("不能将群主移出群聊");
        }
        if (operator.getRole() == GroupMemberRole.admin.getCode() && target.getRole() == GroupMemberRole.admin.getCode()) {
            throw new ServiceException("管理员之间不能互相移出群聊");
        }

        this.removeById(target.getId());

        // 同步悟空 IM 频道订阅者，被移出者不再接收该群消息
        BaseChannel baseChannel = new BaseChannel();
        baseChannel.setChannelId(Constants.GROUP_ID_PREFIX + groupId);
        baseChannel.setSubscribers(Collections.singletonList(uid.toString()));

        wuKongClient.removeSubscribers(baseChannel);

        // 向群频道播报移出通知
        String nickname = resolveNickname(uid);

        chatMessage.sendGroupMessage(
                groupId.toString(),
                MessageType.groupMemberChange,
                nickname + " 已被移出群聊"
        );

        // 单独通知被移出的用户本人：客户端实时提醒并禁止继续发言
        Group group = groupMapper.selectById(groupId);
        String groupName = group != null && !StringUtils.isEmpty(group.getName()) ? group.getName() : "群聊";

        Map<String, Object> extra = new HashMap<>(2);
        extra.put("groupId", Constants.GROUP_ID_PREFIX + groupId);

        chatMessage.sendUserNotice(
                uid,
                MessageType.groupMemberRemoved,
                "群聊通知",
                "你已被移出群聊「" + groupName + "」",
                extra
        );
    }

    /**
     * 群成员禁言/解禁
     *
     * <p>仅群主/管理员可操作，不能对群主禁言；muteMinutes 为空或小于等于 0 时解除禁言。</p>
     * */
    @Override
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
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
