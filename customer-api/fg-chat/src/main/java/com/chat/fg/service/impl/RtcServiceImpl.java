package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chat.fg.config.RtcProperties;
import com.chat.fg.mapper.RtcParticipantMapper;
import com.chat.fg.mapper.RtcRoomMapper;
import com.chat.fg.pojo.dto.CallDTO;
import com.chat.fg.pojo.dto.InviteDTO;
import com.chat.fg.pojo.enums.GroupMemberRole;
import com.chat.fg.pojo.vo.CallVO;
import com.chat.fg.pojo.vo.InviteVO;
import com.chat.fg.pojo.vo.RoomDetailVO;
import com.chat.fg.pojo.vo.RtcMemberVO;
import com.chat.fg.service.IGroupMemberService;
import com.chat.fg.service.IGroupService;
import com.chat.fg.service.IRtcService;
import com.chat.fg.service.IUserService;
import com.chat.fg.service.LivekitAdminService;
import com.chat.fg.utils.DistributedLockHelper;
import com.chat.fg.wk.ChatMessage;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.constant.Constants;
import com.xy.common.entity.User;
import com.xy.common.entity.chat.Group;
import com.xy.common.entity.chat.GroupMember;
import com.xy.common.entity.chat.RtcParticipant;
import com.xy.common.entity.chat.RtcRoom;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 音视频通话 逻辑实现
 *
 * <p>房间状态机（t_rtc_room.status）：
 * 0-呼叫中（在场人数 &lt; 2）→ 1-通话中（在场人数 ≥ 2）→ 2-已结束</p>
 *
 * <p>三个平面：呼叫控制信令走悟空 IM 个人频道（200-205，不持久化），
 * 媒体面由 LiveKit SFU 承载（房间名 = room_id），
 * 状态面为 t_rtc_room / t_rtc_participant（审计与超时兜底）。</p>
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class RtcServiceImpl implements IRtcService {

    /** 房间状态 */
    private static final int ROOM_CALLING = 0;
    private static final int ROOM_ACTIVE = 1;
    private static final int ROOM_ENDED = 2;

    /** 参与者状态 */
    private static final int P_CALLING = 0;
    private static final int P_JOINED = 1;
    private static final int P_LEFT = 2;
    private static final int P_REJECTED = 3;

    /** 房间角色 */
    private static final int ROLE_REGULAR = 0;
    private static final int ROLE_HOST = 1;

    /** 频道类型 */
    private static final int CT_PERSONAL = 1;
    private static final int CT_GROUP = 2;

    /** 单聊房间人数 */
    private static final int PERSONAL_MAX_PARTICIPANTS = 2;

    /** 达到该在场人数即视为通话中 */
    private static final int ACTIVE_MIN_JOINED = 2;

    /** 通话中房间的媒体面全部离场后，仍保留多久（秒）；用于兜底前端崩溃/断网未上报挂断 */
    private static final int ACTIVE_EMPTY_GRACE_SECONDS = 60;

    private final RtcRoomMapper rtcRoomMapper;
    private final RtcParticipantMapper rtcParticipantMapper;
    private final WuKongClient wuKongClient;
    private final IUserService userService;
    private final DistributedLockHelper lockHelper;
    private final RtcProperties rtcProperties;
    private final LivekitAdminService livekitAdminService;
    private final IGroupService groupService;
    private final IGroupMemberService groupMemberService;
    private final ChatMessage chatMessage;

    @Override
    public CallVO call(Long callerUid, CallDTO dto) {
        int channelType = dto.getChannelType() == null ? CT_PERSONAL : dto.getChannelType();
        return channelType == CT_GROUP ? callGroup(callerUid, dto) : callPersonal(callerUid, dto);
    }

    @Override
    public CallVO accept(Long uid, String roomId) {
        return lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            if (room == null || room.getStatus() == ROOM_ENDED) {
                throw new BusinessException("通话已结束");
            }
            RtcParticipant self = getParticipant(roomId, uid);
            if (self == null) {
                throw new BusinessException("你不在该通话中");
            }

            LocalDateTime now = LocalDateTime.now();
            if (self.getStatus() != P_JOINED) {
                rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                        .eq(RtcParticipant::getRoomId, roomId)
                        .eq(RtcParticipant::getUId, uid)
                        .set(RtcParticipant::getStatus, P_JOINED)
                        .set(RtcParticipant::getJoinTime, now)
                        .set(RtcParticipant::getLeaveTime, null));
            }
            activateIfEnough(room, now);

            // 通知发起人：单聊用于停止回铃，群聊用于刷新邀请状态
            pushRtcSignal(Long.parseLong(room.getHostUid()), uid,
                    MessageType.rtcSignalAccept.getCode(), signalContent(room).toJSONString());

            log.info("RTC 接听/接入, roomId={}, uid={}", roomId, uid);
            return sessionVO(room, uid);
        }, "操作过于频繁，请重试");
    }

    @Override
    public void reject(Long uid, String roomId) {
        lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            if (room == null || room.getStatus() == ROOM_ENDED) {
                return null;
            }
            RtcParticipant self = getParticipant(roomId, uid);
            if (self == null || self.getStatus() != P_CALLING) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            JSONObject content = signalContent(room);

            // 群聊：仅本人退出邀请，房间继续
            if (isGroup(room)) {
                updateParticipant(roomId, uid, P_REJECTED, now);
                pushRtcSignal(Long.parseLong(room.getHostUid()), uid,
                        MessageType.rtcSignalReject.getCode(), content.toJSONString());
                log.info("RTC 群通话成员拒绝, roomId={}, uid={}", roomId, uid);
                return null;
            }

            boolean ended = endRoomIfActive(room, now);
            updateParticipant(roomId, uid, P_REJECTED, now);
            endPeerParticipants(roomId, uid, now);

            Long callerUid = Long.parseLong(room.getHostUid());
            pushRtcSignal(callerUid, uid, MessageType.rtcSignalReject.getCode(), content.toJSONString());
            if (ended) {
                pushRecord(callerUid, uid, buildEndRecord(room, "rejected", 0));
            }
            log.info("RTC 拒绝, roomId={}, uid={}", roomId, uid);
            return null;
        }, "操作过于频繁，请重试");
    }

    @Override
    public void hangup(Long uid, String roomId) {
        lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            if (room == null || room.getStatus() == ROOM_ENDED) {
                return null;
            }
            RtcParticipant self = getParticipant(roomId, uid);
            if (self == null) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            JSONObject content = signalContent(room);

            // 群聊：仅本人离开，无人时结束整场
            if (isGroup(room)) {
                updateParticipant(roomId, uid, P_LEFT, now);
                pushRtcSignal(Long.parseLong(room.getHostUid()), uid,
                        MessageType.rtcSignalHangup.getCode(), content.toJSONString());

                if (countJoined(roomId) == 0) {
                    JSONObject record = buildEndRecord(room, eventOf(room, now), durationOf(room, now));
                    boolean ended = endRoomIfActive(room, now);
                    markAllLeft(roomId, now);
                    if (ended) {
                        pushGroupRecord(room, record);
                        broadcastSignal(room, uid, MessageType.rtcSignalHangup.getCode(), content);
                    }
                }
                log.info("RTC 群通话成员离开, roomId={}, uid={}", roomId, uid);
                return null;
            }

            // 单聊：任一方挂断即结束整房
            JSONObject record = buildEndRecord(room, eventOf(room, now), durationOf(room, now));
            boolean ended = endRoomIfActive(room, now);
            updateParticipant(roomId, uid, P_LEFT, now);
            endPeerParticipants(roomId, uid, now);

            Long peerUid = peerOf(room, uid);
            pushRtcSignal(peerUid, uid, MessageType.rtcSignalHangup.getCode(), content.toJSONString());
            if (ended) {
                pushRecord(peerUid, uid, record);
            }
            log.info("RTC 挂断, roomId={}, by={}", roomId, uid);
            return null;
        }, "操作过于频繁，请重试");
    }

    @Override
    public void cancel(Long uid, String roomId) {
        lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            // 仅发起人在呼叫中可取消
            if (room == null || room.getStatus() != ROOM_CALLING
                    || !String.valueOf(uid).equals(room.getHostUid())) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            JSONObject record = buildEndRecord(room, "canceled", 0);
            boolean ended = endRoomIfActive(room, now);
            markAllLeft(roomId, now);

            JSONObject content = signalContent(room);
            if (isGroup(room)) {
                broadcastSignal(room, uid, MessageType.rtcSignalCancel.getCode(), content);
                if (ended) {
                    pushGroupRecord(room, record);
                }
            } else {
                Long calleeUid = Long.parseLong(room.getChannelId());
                pushRtcSignal(calleeUid, uid, MessageType.rtcSignalCancel.getCode(), content.toJSONString());
                if (ended) {
                    pushRecord(calleeUid, uid, record);
                }
            }
            log.info("RTC 取消, roomId={}, caller={}", roomId, uid);
            return null;
        }, "操作过于频繁，请重试");
    }

    @Override
    public InviteVO invite(Long uid, InviteDTO dto) {
        return lockHelper.tryLock("rtc:room:" + dto.getRoomId(), 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(dto.getRoomId());
            if (room == null || room.getStatus() == ROOM_ENDED) {
                throw new BusinessException("通话已结束");
            }
            if (!isGroup(room)) {
                throw new BusinessException("仅群聊通话支持邀请成员");
            }
            RtcParticipant self = getParticipant(room.getRoomId(), uid);
            if (self == null || (self.getStatus() != P_JOINED && self.getStatus() != P_CALLING)) {
                throw new BusinessException("你不在该通话中");
            }
            if (!isRoomAdmin(room, uid)) {
                throw new BusinessException("只有发起人或群主/管理员可以邀请成员");
            }

            List<Long> targets = dto.getInviteeUids().stream()
                    .filter(target -> target != null && !target.equals(uid))
                    .distinct()
                    .toList();
            if (targets.isEmpty()) {
                throw new BusinessException("请选择要邀请的成员");
            }

            Long groupId = parseGroupId(room.getChannelId());
            for (Long target : targets) {
                if (groupMemberService.getGroupMemberByUid(target, groupId) == null) {
                    throw new BusinessException("邀请的成员不在此群聊中");
                }
            }

            // 已在房间且未离开的成员不重复邀请（唯一索引 uk_room_user 决定只能 UPDATE）
            Map<Long, RtcParticipant> existing = new LinkedHashMap<>();
            long occupied = 0;
            for (RtcParticipant p : listActiveParticipants(room.getRoomId())) {
                existing.put(p.getUId(), p);
                occupied++;
            }

            List<Long> toInvite = new ArrayList<>();
            for (Long target : targets) {
                RtcParticipant p = existing.get(target);
                if (p != null && p.getStatus() != P_LEFT && p.getStatus() != P_REJECTED) {
                    continue;
                }
                toInvite.add(target);
            }
            if (toInvite.isEmpty()) {
                throw new BusinessException("所选成员已在通话中");
            }
            if (occupied + toInvite.size() > room.getMaxParticipants()) {
                throw new BusinessException("通话人数已达上限（" + room.getMaxParticipants() + " 人）");
            }

            for (Long target : toInvite) {
                if (existing.containsKey(target)) {
                    rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                            .eq(RtcParticipant::getRoomId, room.getRoomId())
                            .eq(RtcParticipant::getUId, target)
                            .set(RtcParticipant::getStatus, P_CALLING)
                            .set(RtcParticipant::getJoinTime, null)
                            .set(RtcParticipant::getLeaveTime, null));
                } else {
                    rtcParticipantMapper.insert(participant(room.getRoomId(), target, ROLE_REGULAR, P_CALLING, null));
                }
                pushInvite(room, target, uid);
            }

            log.info("RTC 群通话追加邀请, roomId={}, by={}, inviteeCount={}", room.getRoomId(), uid, toInvite.size());

            InviteVO vo = new InviteVO();
            vo.setRoomId(room.getRoomId());
            vo.setMaxParticipants(room.getMaxParticipants());
            vo.setParticipants(listMembers(room));
            return vo;
        }, "操作过于频繁，请重试");
    }

    @Override
    public void end(Long uid, String roomId) {
        Boolean ended = lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            if (room == null || room.getStatus() == ROOM_ENDED) {
                return false;
            }
            if (!isGroup(room)) {
                throw new BusinessException("单聊通话请使用挂断结束");
            }
            if (!isRoomAdmin(room, uid)) {
                throw new BusinessException("只有发起人或群主/管理员可以结束通话");
            }

            LocalDateTime now = LocalDateTime.now();
            JSONObject record = buildEndRecord(room, eventOf(room, now), durationOf(room, now));
            boolean done = endRoomIfActive(room, now);
            markAllLeft(roomId, now);
            if (done) {
                pushGroupRecord(room, record);
                broadcastSignal(room, uid, MessageType.rtcSignalHangup.getCode(), signalContent(room));
            }
            log.info("RTC 结束群通话, roomId={}, by={}", roomId, uid);
            return done;
        }, "操作过于频繁，请重试");

        if (Boolean.TRUE.equals(ended)) {
            livekitAdminService.closeRoom(roomId);
        }
    }

    @Override
    public CallVO token(Long uid, String roomId) {
        RtcRoom room = rtcRoomMapper.selectById(roomId);
        if (room == null || room.getStatus() == ROOM_ENDED) {
            throw new BusinessException("通话已结束");
        }
        RtcParticipant self = getParticipant(roomId, uid);
        if (self == null || self.getStatus() == P_LEFT || self.getStatus() == P_REJECTED) {
            throw new BusinessException("你不在该通话中");
        }
        return sessionVO(room, uid);
    }

    @Override
    public RoomDetailVO roomDetail(Long uid, String roomId) {
        RtcRoom room = rtcRoomMapper.selectById(roomId);
        if (room == null) {
            throw new BusinessException("通话不存在");
        }
        RtcParticipant self = getParticipant(roomId, uid);
        if (self == null) {
            throw new BusinessException("你不在该通话中");
        }

        return toRoomDetail(room);
    }

    @Override
    public boolean isLivekitUp() {
        return livekitAdminService.isUp();
    }

    @Override
    public int expireStaleRooms() {
        Set<String> toClose = new HashSet<>();
        int count = expireNeverStarted(toClose) + expireAbandoned(toClose) + expireOverlong(toClose);
        // LiveKit 调用放在锁外，避免网络耗时占满分布式锁租约
        for (String roomId : toClose) {
            livekitAdminService.closeRoom(roomId);
        }
        return count;
    }

    // ==================== 发起通话 ====================

    private CallVO callPersonal(Long callerUid, CallDTO dto) {
        Long calleeUid = dto.getCalleeUid();
        if (calleeUid == null) {
            throw new BusinessException("被叫用户不能为空");
        }
        if (calleeUid.equals(callerUid)) {
            throw new BusinessException("不能呼叫自己");
        }

        RtcRoom room = newRoom(callerUid, CT_PERSONAL, String.valueOf(calleeUid),
                PERSONAL_MAX_PARTICIPANTS, dto.getMediaType());
        livekitAdminService.ensureRoom(room.getRoomId(), room.getMaxParticipants());

        try {
            return lockHelper.tryLock("rtc:call:" + calleeUid, 3, 10, () -> {
                if (hasActiveRoom(calleeUid)) {
                    throw new BusinessException("对方忙线，请稍后再试");
                }
                if (hasActiveRoom(callerUid)) {
                    throw new BusinessException("你正处于通话中，请先结束当前通话");
                }

                rtcRoomMapper.insert(room);
                rtcParticipantMapper.insert(participant(room.getRoomId(), callerUid, ROLE_HOST, P_JOINED, LocalDateTime.now()));
                rtcParticipantMapper.insert(participant(room.getRoomId(), calleeUid, ROLE_REGULAR, P_CALLING, null));

                pushInvite(room, calleeUid, callerUid);

                log.info("RTC 发起单聊呼叫, roomId={}, caller={}, callee={}, mediaType={}",
                        room.getRoomId(), callerUid, calleeUid, dto.getMediaType());
                return sessionVO(room, callerUid);
            }, "对方正在接听其它来电");
        } catch (RuntimeException e) {
            // 呼叫未成立（忙线/冲突）时回收已创建的空房间
            livekitAdminService.closeRoom(room.getRoomId());
            throw e;
        }
    }

    private CallVO callGroup(Long callerUid, CallDTO dto) {
        if (StringUtils.isEmpty(dto.getChannelId())) {
            throw new BusinessException("群聊频道不能为空");
        }
        Group group = groupService.findById(dto.getChannelId());
        if (group == null) {
            throw new BusinessException("群聊不存在");
        }
        if (group.getDisband() != null && group.getDisband() == 1) {
            throw new BusinessException("群聊已解散");
        }
        if (group.getIsBan() != null && group.getIsBan() == 1) {
            throw new BusinessException("群聊已开启全员禁言，无法发起通话");
        }
        if (groupMemberService.getGroupMemberByUid(callerUid, group.getGroupId()) == null) {
            throw new BusinessException("你不在该群聊中");
        }

        List<Long> invitees = dto.getInviteeUids() == null ? List.of() : dto.getInviteeUids().stream()
                .filter(invitee -> invitee != null && !invitee.equals(callerUid))
                .distinct()
                .toList();
        if (invitees.isEmpty()) {
            throw new BusinessException("请选择至少一位成员");
        }

        int maxParticipants = rtcProperties.getMaxParticipants();
        if (invitees.size() + 1 > maxParticipants) {
            throw new BusinessException("群通话最多 " + maxParticipants + " 人");
        }
        for (Long invitee : invitees) {
            if (groupMemberService.getGroupMemberByUid(invitee, group.getGroupId()) == null) {
                throw new BusinessException("邀请的成员不在此群聊中");
            }
        }

        String channelId = Constants.GROUP_ID_PREFIX + group.getGroupId();
        RtcRoom room = newRoom(callerUid, CT_GROUP, channelId, maxParticipants, dto.getMediaType());
        livekitAdminService.ensureRoom(room.getRoomId(), room.getMaxParticipants());

        try {
            return lockHelper.tryLock("rtc:call:" + callerUid, 3, 10, () -> {
                if (hasActiveRoom(callerUid)) {
                    throw new BusinessException("你正处于通话中，请先结束当前通话");
                }
                Long ongoing = rtcRoomMapper.selectCount(new LambdaQueryWrapper<RtcRoom>()
                        .eq(RtcRoom::getChannelId, channelId)
                        .eq(RtcRoom::getChannelType, CT_GROUP)
                        .ne(RtcRoom::getStatus, ROOM_ENDED));
                if (ongoing != null && ongoing > 0) {
                    throw new BusinessException("该群已有进行中的通话");
                }

                rtcRoomMapper.insert(room);
                rtcParticipantMapper.insert(participant(room.getRoomId(), callerUid, ROLE_HOST, P_JOINED, LocalDateTime.now()));
                for (Long invitee : invitees) {
                    rtcParticipantMapper.insert(participant(room.getRoomId(), invitee, ROLE_REGULAR, P_CALLING, null));
                }
                for (Long invitee : invitees) {
                    pushInvite(room, invitee, callerUid);
                }

                log.info("RTC 发起群聊通话, roomId={}, groupId={}, host={}, inviteeCount={}",
                        room.getRoomId(), group.getGroupId(), callerUid, invitees.size());
                return sessionVO(room, callerUid);
            }, "操作过于频繁，请重试");
        } catch (RuntimeException e) {
            livekitAdminService.closeRoom(room.getRoomId());
            throw e;
        }
    }

    // ==================== 超时清理 ====================

    /**
     * 阶段 A：呼叫中且超时未接听 → 结束房间、通知全部参与者并落"未接听"记录
     * */
    private int expireNeverStarted(Set<String> toClose) {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(rtcProperties.getCallTimeoutSeconds());
        List<RtcRoom> stale = rtcRoomMapper.selectList(new LambdaQueryWrapper<RtcRoom>()
                .eq(RtcRoom::getStatus, ROOM_CALLING)
                .lt(RtcRoom::getCreateTime, deadline));

        int count = 0;
        for (RtcRoom room : stale) {
            // 呼叫中追加邀请过成员时，以最近一条参与者记录为超时基准
            LocalDateTime lastJoin = lastParticipantCreateTime(room.getRoomId());
            if (lastJoin != null && lastJoin.isAfter(deadline)) {
                continue;
            }
            Boolean done = lockHelper.tryLock("rtc:room:" + room.getRoomId(), 3, 10, () -> {
                RtcRoom fresh = rtcRoomMapper.selectById(room.getRoomId());
                if (fresh == null || fresh.getStatus() != ROOM_CALLING) {
                    return false;
                }
                LocalDateTime now = LocalDateTime.now();
                JSONObject record = buildEndRecord(fresh, "missed", 0);
                boolean ended = endRoomIfActive(fresh, now);
                markAllLeft(fresh.getRoomId(), now);
                if (ended) {
                    broadcastSignal(fresh, null, MessageType.rtcSignalCancel.getCode(), signalContent(fresh));
                    if (isGroup(fresh)) {
                        pushGroupRecord(fresh, record);
                    } else {
                        pushRecord(Long.parseLong(fresh.getChannelId()), Long.parseLong(fresh.getHostUid()), record);
                    }
                    log.info("RTC 呼叫超时清理, roomId={}", fresh.getRoomId());
                }
                return ended;
            }, "房间清理冲突，跳过");
            if (Boolean.TRUE.equals(done)) {
                count++;
                toClose.add(room.getRoomId());
            }
        }
        return count;
    }

    /**
     * 阶段 B：通话中但媒体面已无人在场 → 结束房间
     * 兜底前端崩溃、断网、媒体建连失败等异常退出未上报挂断的场景
     * */
    private int expireAbandoned(Set<String> toClose) {
        LocalDateTime limit = LocalDateTime.now().minusSeconds(ACTIVE_EMPTY_GRACE_SECONDS);
        List<RtcRoom> active = rtcRoomMapper.selectList(new LambdaQueryWrapper<RtcRoom>()
                .eq(RtcRoom::getStatus, ROOM_ACTIVE)
                .isNotNull(RtcRoom::getStartTime)
                .lt(RtcRoom::getStartTime, limit));

        int count = 0;
        for (RtcRoom room : active) {
            List<String> alive;
            try {
                alive = livekitAdminService.listParticipantIdentities(room.getRoomId());
            } catch (Exception e) {
                // LiveKit 不可达时跳过，避免误杀进行中的通话
                log.warn("房间在场校验跳过（LiveKit 不可达）, roomId={}", room.getRoomId(), e);
                continue;
            }
            if (!alive.isEmpty()) {
                continue;
            }
            if (closeActiveRoom(room.getRoomId(), "abandoned")) {
                count++;
                toClose.add(room.getRoomId());
            }
        }
        return count;
    }

    /**
     * 阶段 C：通话中超过最大时长 → 以 LiveKit 在场情况为准结束或修正参与者
     * */
    private int expireOverlong(Set<String> toClose) {
        LocalDateTime limit = LocalDateTime.now().minusMinutes(rtcProperties.getMaxCallMinutes());
        List<RtcRoom> overlong = rtcRoomMapper.selectList(new LambdaQueryWrapper<RtcRoom>()
                .eq(RtcRoom::getStatus, ROOM_ACTIVE)
                .isNotNull(RtcRoom::getStartTime)
                .lt(RtcRoom::getStartTime, limit));

        int count = 0;
        for (RtcRoom room : overlong) {
            List<String> alive;
            try {
                alive = livekitAdminService.listParticipantIdentities(room.getRoomId());
            } catch (Exception e) {
                // LiveKit 不可达时跳过，避免误杀进行中的通话
                log.warn("通话超时校验跳过（LiveKit 不可达）, roomId={}", room.getRoomId(), e);
                continue;
            }

            if (!alive.isEmpty()) {
                // 房间仍在进行：仅修正 DB 中已不在 LiveKit 的参与者
                LocalDateTime now = LocalDateTime.now();
                for (RtcParticipant p : listActiveParticipants(room.getRoomId())) {
                    if (!alive.contains(String.valueOf(p.getUId()))) {
                        updateParticipant(room.getRoomId(), p.getUId(), P_LEFT, now);
                    }
                }
                continue;
            }

            if (closeActiveRoom(room.getRoomId(), "overlong")) {
                count++;
                toClose.add(room.getRoomId());
            }
        }
        return count;
    }

    /**
     * 结束一个仍处于通话中的房间（幂等，并发时以抢占到锁的一方为准）
     * */
    private boolean closeActiveRoom(String roomId, String reason) {
        Boolean done = lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom fresh = rtcRoomMapper.selectById(roomId);
            if (fresh == null || fresh.getStatus() != ROOM_ACTIVE) {
                return false;
            }
            LocalDateTime now = LocalDateTime.now();
            JSONObject record = buildEndRecord(fresh, eventOf(fresh, now), durationOf(fresh, now));
            boolean ended = endRoomIfActive(fresh, now);
            markAllLeft(roomId, now);
            if (ended) {
                broadcastSignal(fresh, null, MessageType.rtcSignalHangup.getCode(), signalContent(fresh));
                if (isGroup(fresh)) {
                    pushGroupRecord(fresh, record);
                } else {
                    pushRecord(Long.parseLong(fresh.getChannelId()), Long.parseLong(fresh.getHostUid()), record);
                }
                log.info("RTC 通话结束, roomId={}, reason={}", roomId, reason);
            }
            return ended;
        }, "房间清理冲突，跳过");
        return Boolean.TRUE.equals(done);
    }

    // ==================== 内部工具 ====================

    private RtcRoom newRoom(Long hostUid, int channelType, String channelId, int maxParticipants, Integer mediaType) {
        RtcRoom room = new RtcRoom();
        room.setRoomId(UUID.randomUUID().toString().replace("-", ""));
        room.setRoomType(String.valueOf(mediaType));
        room.setHostUid(String.valueOf(hostUid));
        room.setChannelId(channelId);
        room.setChannelType(channelType);
        room.setMaxParticipants(maxParticipants);
        room.setStatus(ROOM_CALLING);
        room.setSfuNode(rtcProperties.getLivekit().getUrl());
        return room;
    }

    private RtcParticipant participant(String roomId, Long uid, int role, int status, LocalDateTime joinTime) {
        RtcParticipant p = new RtcParticipant();
        p.setRoomId(roomId);
        p.setUId(uid);
        p.setRole(role);
        p.setStatus(status);
        p.setJoinTime(joinTime);
        return p;
    }

    /**
     * 组装会话信息（含 LiveKit 接入 token）
     * */
    private CallVO sessionVO(RtcRoom room, Long uid) {
        CallVO vo = new CallVO();
        vo.setRoomId(room.getRoomId());
        vo.setMediaType(parseMediaType(room.getRoomType()));
        vo.setChannelType(channelTypeOf(room));
        vo.setChannelId(room.getChannelId());
        vo.setLivekitUrl(rtcProperties.getLivekit().getUrl());
        vo.setMaxParticipants(room.getMaxParticipants());

        User user = userService.getUserInfo(uid);
        JSONObject metadata = new JSONObject();
        metadata.put("uid", uid);
        metadata.put("nickname", user != null ? user.getNickname() : "");
        metadata.put("avatar", user != null ? user.getAvatar() : "");

        vo.setToken(livekitAdminService.issueToken(
                room.getRoomId(), uid, displayName(room, uid), metadata.toJSONString(), isRoomAdmin(room, uid)));
        vo.setParticipants(listMembers(room));
        return vo;
    }

    private RoomDetailVO toRoomDetail(RtcRoom room) {
        RoomDetailVO vo = new RoomDetailVO();
        vo.setRoomId(room.getRoomId());
        vo.setMediaType(parseMediaType(room.getRoomType()));
        vo.setChannelType(channelTypeOf(room));
        vo.setChannelId(room.getChannelId());
        vo.setHostUid(room.getHostUid());
        vo.setStatus(room.getStatus());
        vo.setMaxParticipants(room.getMaxParticipants());
        vo.setLivekitUrl(rtcProperties.getLivekit().getUrl());
        vo.setStartTime(room.getStartTime());
        vo.setParticipants(listMembers(room));
        return vo;
    }

    /**
     * 房间内在场人数达到下限时把房间推进为通话中
     * */
    private void activateIfEnough(RtcRoom room, LocalDateTime now) {
        if (room.getStatus() != ROOM_CALLING) {
            return;
        }
        if (countJoined(room.getRoomId()) < ACTIVE_MIN_JOINED) {
            return;
        }
        int rows = rtcRoomMapper.update(null, new LambdaUpdateWrapper<RtcRoom>()
                .eq(RtcRoom::getRoomId, room.getRoomId())
                .eq(RtcRoom::getStatus, ROOM_CALLING)
                .set(RtcRoom::getStatus, ROOM_ACTIVE)
                .set(RtcRoom::getStartTime, now));
        if (rows > 0) {
            room.setStatus(ROOM_ACTIVE);
            room.setStartTime(now);
        }
    }

    /**
     * 结束房间：以受影响行数判定"赢得迁移"，保证通话记录只推送一次
     * */
    private boolean endRoomIfActive(RtcRoom room, LocalDateTime now) {
        int rows = rtcRoomMapper.update(null, new LambdaUpdateWrapper<RtcRoom>()
                .eq(RtcRoom::getRoomId, room.getRoomId())
                .ne(RtcRoom::getStatus, ROOM_ENDED)
                .set(RtcRoom::getStatus, ROOM_ENDED)
                .set(RtcRoom::getEndTime, now));
        return rows > 0;
    }

    private void updateParticipant(String roomId, Long uid, int status, LocalDateTime leaveTime) {
        rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .eq(RtcParticipant::getUId, uid)
                .set(RtcParticipant::getStatus, status)
                .set(RtcParticipant::getLeaveTime, leaveTime));
    }

    /**
     * 将房间内除指定用户外的其余参与者置为已离开
     *
     * <p>单聊挂断/拒绝时若只更新发起方，对端参与者会残留为"呼叫中/已加入"，
     * 导致 hasActiveRoom 误判其长期忙线。</p>
     * */
    private void endPeerParticipants(String roomId, Long exceptUid, LocalDateTime now) {
        rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .ne(RtcParticipant::getUId, exceptUid)
                .set(RtcParticipant::getStatus, P_LEFT)
                .set(RtcParticipant::getLeaveTime, now));
    }

    private void markAllLeft(String roomId, LocalDateTime now) {
        rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .ne(RtcParticipant::getStatus, P_LEFT)
                .set(RtcParticipant::getStatus, P_LEFT)
                .set(RtcParticipant::getLeaveTime, now));
    }

    /**
     * 用户是否有活跃房间（呼叫中/通话中）
     *
     * <p>群聊中被邀请但未接入（P_CALLING）不占用忙线状态，
     * 否则被拉入群通话后未接听会长期无法发起其它通话。</p>
     * */
    private boolean hasActiveRoom(Long uid) {
        List<RtcParticipant> participants = rtcParticipantMapper.selectList(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getUId, uid)
                .in(RtcParticipant::getStatus, P_CALLING, P_JOINED));
        if (participants.isEmpty()) {
            return false;
        }
        List<String> roomIds = participants.stream().map(RtcParticipant::getRoomId).distinct().toList();
        List<RtcRoom> rooms = rtcRoomMapper.selectList(new LambdaQueryWrapper<RtcRoom>()
                .in(RtcRoom::getRoomId, roomIds)
                .ne(RtcRoom::getStatus, ROOM_ENDED));
        if (rooms.isEmpty()) {
            return false;
        }
        Map<String, RtcRoom> roomMap = new LinkedHashMap<>();
        for (RtcRoom room : rooms) {
            roomMap.put(room.getRoomId(), room);
        }
        for (RtcParticipant p : participants) {
            RtcRoom room = roomMap.get(p.getRoomId());
            if (room == null) {
                continue;
            }
            if (p.getStatus() == P_JOINED || !isGroup(room)) {
                return true;
            }
        }
        return false;
    }

    private RtcParticipant getParticipant(String roomId, Long uid) {
        return rtcParticipantMapper.selectOne(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .eq(RtcParticipant::getUId, uid));
    }

    /**
     * 房间内未离开的参与者（呼叫中 + 已加入）
     * */
    private List<RtcParticipant> listActiveParticipants(String roomId) {
        return rtcParticipantMapper.selectList(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .in(RtcParticipant::getStatus, P_CALLING, P_JOINED));
    }

    private long countJoined(String roomId) {
        Long count = rtcParticipantMapper.selectCount(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .eq(RtcParticipant::getStatus, P_JOINED));
        return count == null ? 0 : count;
    }

    private LocalDateTime lastParticipantCreateTime(String roomId) {
        List<Object> values = rtcParticipantMapper.selectObjs(new QueryWrapper<RtcParticipant>()
                .select("MAX(create_time)")
                .eq("room_id", roomId));
        if (values == null || values.isEmpty() || values.get(0) == null) {
            return null;
        }
        Object value = values.get(0);
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    /**
     * 房间成员视图（呼叫中 + 已加入）
     * */
    private List<RtcMemberVO> listMembers(RtcRoom room) {
        Long groupId = isGroup(room) ? parseGroupId(room.getChannelId()) : null;
        List<RtcMemberVO> members = new ArrayList<>();
        for (RtcParticipant p : listActiveParticipants(room.getRoomId())) {
            RtcMemberVO member = new RtcMemberVO();
            member.setUid(p.getUId());
            member.setRole(p.getRole());
            member.setStatus(p.getStatus());
            member.setJoinTime(p.getJoinTime());
            User user = userService.getUserInfo(p.getUId());
            if (user != null) {
                member.setNickname(user.getNickname());
                member.setAvatar(user.getAvatar());
            }
            if (groupId != null) {
                GroupMember gm = groupMemberService.getGroupMemberByUid(p.getUId(), groupId);
                if (gm != null) {
                    member.setGroupNickname(gm.getGroupNickname());
                }
            }
            members.add(member);
        }
        // 主持人优先，其次按加入时间
        members.sort(Comparator.comparing(RtcMemberVO::getRole).reversed()
                .thenComparing(RtcMemberVO::getStatus)
                .thenComparing(m -> m.getJoinTime() == null ? LocalDateTime.MAX : m.getJoinTime()));
        return members;
    }

    private String displayName(RtcRoom room, Long uid) {
        if (isGroup(room)) {
            GroupMember gm = groupMemberService.getGroupMemberByUid(uid, parseGroupId(room.getChannelId()));
            if (gm != null && StringUtils.isNotEmpty(gm.getGroupNickname())) {
                return gm.getGroupNickname();
            }
        }
        User user = userService.getUserInfo(uid);
        return user != null && user.getNickname() != null ? user.getNickname() : "";
    }

    /**
     * 是否具备房间管理权限：发起人，或群聊中的群主/管理员
     * */
    private boolean isRoomAdmin(RtcRoom room, Long uid) {
        if (String.valueOf(uid).equals(room.getHostUid())) {
            return true;
        }
        if (!isGroup(room)) {
            return false;
        }
        GroupMember gm = groupMemberService.getGroupMemberByUid(uid, parseGroupId(room.getChannelId()));
        if (gm == null) {
            return false;
        }
        return gm.getRole() == GroupMemberRole.admin.getCode() || gm.getRole() == GroupMemberRole.owner.getCode();
    }

    private boolean isGroup(RtcRoom room) {
        return room.getChannelType() != null && room.getChannelType() == CT_GROUP;
    }

    private int channelTypeOf(RtcRoom room) {
        return room.getChannelType() == null ? CT_PERSONAL : room.getChannelType();
    }

    private Long parseGroupId(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            return null;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(channelId);
        return matcher.find() ? Long.parseLong(matcher.group()) : null;
    }

    /**
     * 单聊房间内对端用户：hostUid 存主叫、channelId 存被叫
     * */
    private Long peerOf(RtcRoom room, Long uid) {
        return String.valueOf(uid).equals(room.getHostUid())
                ? Long.parseLong(room.getChannelId())
                : Long.parseLong(room.getHostUid());
    }

    private int parseMediaType(String roomType) {
        try {
            return Integer.parseInt(roomType);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private long durationOf(RtcRoom room, LocalDateTime now) {
        if (room.getStatus() == ROOM_ACTIVE && room.getStartTime() != null) {
            return Duration.between(room.getStartTime(), now).getSeconds();
        }
        return 0;
    }

    /**
     * 挂断时的记录事件：接通满 3 秒记 completed，否则记 failed
     * */
    private String eventOf(RtcRoom room, LocalDateTime now) {
        return durationOf(room, now) >= 3 ? "completed" : "failed";
    }

    /**
     * 组装通话记录内容（成员数取有加入时间的参与者数）
     * */
    private JSONObject buildEndRecord(RtcRoom room, String event, long duration) {
        JSONObject record = new JSONObject();
        record.put("roomId", room.getRoomId());
        record.put("mediaType", parseMediaType(room.getRoomType()));
        record.put("event", event);
        record.put("duration", duration);
        record.put("memberCount", joinTimeMemberCount(room.getRoomId()));
        return record;
    }

    private long joinTimeMemberCount(String roomId) {
        Long count = rtcParticipantMapper.selectCount(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .isNotNull(RtcParticipant::getJoinTime));
        return count == null ? 0 : count;
    }

    /**
     * 呼叫/挂断等信令的统一内容体
     * */
    private JSONObject signalContent(RtcRoom room) {
        JSONObject content = new JSONObject();
        content.put("roomId", room.getRoomId());
        content.put("mediaType", parseMediaType(room.getRoomType()));
        content.put("channelType", channelTypeOf(room));
        content.put("channelId", room.getChannelId());
        return content;
    }

    /**
     * 向被邀请人推送邀请信令（携带来电渲染所需信息）
     * */
    private void pushInvite(RtcRoom room, Long toUid, Long fromUid) {
        JSONObject content = signalContent(room);
        content.put("hostUid", room.getHostUid());
        content.put("maxParticipants", room.getMaxParticipants());
        if (isGroup(room)) {
            Group group = groupService.findById(room.getChannelId());
            content.put("groupName", group != null ? group.getName() : "");
            content.put("inviteeCount", room.getMaxParticipants() == 0 ? 0 : countJoined(room.getRoomId()));
        }
        User caller = userService.getUserInfo(fromUid);
        JSONObject callerInfo = new JSONObject();
        callerInfo.put("uid", fromUid);
        callerInfo.put("nickname", caller != null ? caller.getNickname() : "");
        callerInfo.put("avatar", caller != null ? caller.getAvatar() : "");
        content.put("caller", callerInfo);
        pushRtcSignal(toUid, fromUid, MessageType.rtcSignalInvite.getCode(), content.toJSONString());
    }

    /**
     * 向房间内除指定用户外的所有参与者推送信令
     * */
    private void broadcastSignal(RtcRoom room, Long exceptUid, Integer type, JSONObject content) {
        Long fromUid = Long.parseLong(room.getHostUid());
        for (RtcParticipant p : listActiveParticipants(room.getRoomId())) {
            if (exceptUid != null && exceptUid.equals(p.getUId())) {
                continue;
            }
            pushRtcSignal(p.getUId(), fromUid, type, content.toJSONString());
        }
    }

    /**
     * 经悟空 IM 向目标用户推送 RTC 信令
     *
     * @param toUid   接收方
     * @param fromUid 发送方（对端用户）
     * @param type    信令类型（MessageType 200+）
     * @param content 信令内容 JSON 字符串
     * */
    private void pushRtcSignal(Long toUid, Long fromUid, Integer type, String content) {
        JSONObject payload = new JSONObject();
        payload.put("type", type);
        payload.put("content", content);
        String payloadBase64 = Base64.getEncoder()
                .encodeToString(payload.toJSONString().getBytes(StandardCharsets.UTF_8));

        MessageSendRequest request = new MessageSendRequest();
        request.setFromUid(String.valueOf(fromUid));
        request.setChannelId(String.valueOf(toUid));
        request.setChannelType(ChannelType.PERSONAL_CHANNEL.getCode());
        request.setPayload(payloadBase64);
        request.setClientMsgNo(UUID.randomUUID().toString().replace("-", ""));

        // 通话记录需持久化并显示红点；实时信令不持久化（离线不可达，由超时机制兜底）
        boolean isRecord = MessageType.rtcRecord.getCode().equals(type);
        MessageSendRequest.MessageHeader header = new MessageSendRequest.MessageHeader();
        header.setNoPersist(isRecord ? 0 : 1);
        header.setRedDot(isRecord ? 1 : 0);
        header.setSyncOnce(0);
        request.setHeader(header);

        try {
            wuKongClient.sendMessage(request);
        } catch (Exception e) {
            log.error("推送 RTC 信令失败, toUid={}, type={}", toUid, type, e);
        }
    }

    /**
     * 推送单聊通话记录（type=210，持久化）
     * */
    private void pushRecord(Long toUid, Long fromUid, JSONObject record) {
        pushRtcSignal(toUid, fromUid, MessageType.rtcRecord.getCode(), buildRecordText(record, false));
    }

    /**
     * 推送群通话记录（type=210，落在群频道，不受群禁言校验影响）
     * */
    private void pushGroupRecord(RtcRoom room, JSONObject record) {
        Long groupId = parseGroupId(room.getChannelId());
        if (groupId == null) {
            return;
        }
        try {
            chatMessage.sendGroupMessage(String.valueOf(groupId), null,
                    MessageType.rtcRecord, buildRecordText(record, true));
        } catch (Exception e) {
            log.error("推送群通话记录失败, roomId={}", room.getRoomId(), e);
        }
    }

    /**
     * 生成通话记录展示文本
     * */
    private String buildRecordText(JSONObject record, boolean group) {
        boolean video = record.getInteger("mediaType") != null && record.getInteger("mediaType") == 2;
        String media = video ? "视频通话" : "语音通话";
        long memberCount = record.getLongValue("memberCount");
        String members = group && memberCount > 1 ? " · " + memberCount + "人" : "";
        String event = record.getString("event");
        if (event == null) {
            return media + members + " · 未接通";
        }
        return switch (event) {
            case "completed" -> media + members + " · 通话时长 " + formatDuration(record.getLongValue("duration"));
            case "missed" -> media + members + " · 未接听";
            case "canceled" -> media + members + " · 已取消";
            case "rejected" -> media + members + " · 对方已拒绝";
            default -> media + members + " · 未接通";
        };
    }

    private String formatDuration(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
