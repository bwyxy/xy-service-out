package com.chat.fg.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.chat.fg.config.RtcProperties;
import com.chat.fg.mapper.RtcParticipantMapper;
import com.chat.fg.mapper.RtcRoomMapper;
import com.chat.fg.pojo.dto.CallDTO;
import com.chat.fg.pojo.vo.CallVO;
import com.chat.fg.pojo.vo.IceServerVO;
import com.chat.fg.service.IRtcService;
import com.chat.fg.service.IUserService;
import com.chat.fg.utils.DistributedLockHelper;
import com.xy.common.client.WuKongClient;
import com.xy.common.client.dto.request.MessageSendRequest;
import com.xy.common.entity.User;
import com.xy.common.entity.chat.RtcParticipant;
import com.xy.common.entity.chat.RtcRoom;
import com.xy.common.enums.ChannelType;
import com.xy.common.enums.MessageType;
import com.xy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * 音视频通话 逻辑实现
 *
 * <p>房间状态机（t_rtc_room.status）：
 * 0-呼叫中 → 1-通话中（被叫接听）→ 2-已结束（挂断/拒绝/取消/超时）</p>
 *
 * <p>信令经悟空 IM 个人频道推送（payload=Base64({type, content})）：
 * 实时信令(200-208)不持久化，通话记录(210)持久化并显示红点。</p>
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

    private final RtcRoomMapper rtcRoomMapper;
    private final RtcParticipantMapper rtcParticipantMapper;
    private final WuKongClient wuKongClient;
    private final IUserService userService;
    private final DistributedLockHelper lockHelper;
    private final RtcProperties rtcProperties;

    @Override
    public CallVO call(Long callerUid, CallDTO dto) {
        Long calleeUid = dto.getCalleeUid();
        if (calleeUid.equals(callerUid)) {
            throw new BusinessException("不能呼叫自己");
        }

        // 锁被叫维度，防止并发呼叫同一人
        return lockHelper.tryLock("rtc:call:" + calleeUid, 3, 10, () -> {
            if (hasActiveRoom(calleeUid)) {
                throw new BusinessException("对方忙线，请稍后再试");
            }
            if (hasActiveRoom(callerUid)) {
                throw new BusinessException("你正处于通话中，请先结束当前通话");
            }

            RtcRoom room = new RtcRoom();
            room.setRoomId(UUID.randomUUID().toString().replace("-", ""));
            room.setRoomType(String.valueOf(dto.getMediaType()));
            room.setHostUid(String.valueOf(callerUid));
            room.setChannelId(String.valueOf(calleeUid));
            room.setMaxParticipants(2);
            room.setStatus(ROOM_CALLING);
            rtcRoomMapper.insert(room);

            rtcParticipantMapper.insert(participant(room.getRoomId(), callerUid));
            rtcParticipantMapper.insert(participant(room.getRoomId(), calleeUid));

            // 邀请信令携带主叫信息，被叫端据此渲染来电弹窗
            User callerUser = userService.getUserInfo(callerUid);
            JSONObject content = new JSONObject();
            content.put("roomId", room.getRoomId());
            content.put("mediaType", dto.getMediaType());
            JSONObject callerInfo = new JSONObject();
            callerInfo.put("uid", callerUid);
            callerInfo.put("nickname", callerUser != null ? callerUser.getNickname() : "");
            callerInfo.put("avatar", callerUser != null ? callerUser.getAvatar() : "");
            content.put("caller", callerInfo);
            pushRtcSignal(calleeUid, callerUid, MessageType.rtcSignalInvite.getCode(), content.toJSONString());

            log.info("RTC 发起呼叫, roomId={}, caller={}, callee={}, mediaType={}",
                    room.getRoomId(), callerUid, calleeUid, dto.getMediaType());

            CallVO vo = new CallVO();
            vo.setRoomId(room.getRoomId());
            vo.setMediaType(dto.getMediaType());
            return vo;
        }, "对方正在接听其它来电");
    }

    @Override
    public void accept(Long uid, String roomId) {
        lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            // 幂等：房间不存在或已不在呼叫中（可能已被取消/超时）直接返回
            if (room == null || room.getStatus() != ROOM_CALLING) {
                return null;
            }
            RtcParticipant self = getParticipant(roomId, uid);
            if (self == null || self.getStatus() != P_CALLING) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            rtcRoomMapper.update(null, new LambdaUpdateWrapper<RtcRoom>()
                    .eq(RtcRoom::getRoomId, roomId)
                    .set(RtcRoom::getStatus, ROOM_ACTIVE)
                    .set(RtcRoom::getStartTime, now));
            rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                    .eq(RtcParticipant::getRoomId, roomId)
                    .eq(RtcParticipant::getUId, uid)
                    .set(RtcParticipant::getStatus, P_JOINED)
                    .set(RtcParticipant::getJoinTime, now));

            Long callerUid = Long.parseLong(room.getHostUid());
            JSONObject content = new JSONObject();
            content.put("roomId", roomId);
            content.put("mediaType", parseMediaType(room.getRoomType()));
            pushRtcSignal(callerUid, uid, MessageType.rtcSignalAccept.getCode(), content.toJSONString());

            log.info("RTC 接听, roomId={}, callee={}", roomId, uid);
            return null;
        }, "操作过于频繁，请重试");
    }

    @Override
    public void reject(Long uid, String roomId) {
        lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            if (room == null || room.getStatus() != ROOM_CALLING) {
                return null;
            }
            RtcParticipant self = getParticipant(roomId, uid);
            if (self == null || self.getStatus() != P_CALLING) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            endRoom(roomId, now);
            updateParticipant(roomId, uid, P_REJECTED, now);
            // 主叫参与者一并置为已离开，否则其会一直被视为"呼叫中"（hasActiveRoom 判定为忙线）
            endPeerParticipants(roomId, uid, now);

            Long callerUid = Long.parseLong(room.getHostUid());
            JSONObject content = new JSONObject();
            content.put("roomId", roomId);
            pushRtcSignal(callerUid, uid, MessageType.rtcSignalReject.getCode(), content.toJSONString());

            JSONObject record = new JSONObject();
            record.put("event", "rejected");
            record.put("roomId", roomId);
            record.put("mediaType", parseMediaType(room.getRoomType()));
            // 通话记录只由触发操作的一方发送（避免双方各发一条导致会话内重复）
            pushRecord(callerUid, uid, record);

            log.info("RTC 拒绝, roomId={}, callee={}", roomId, uid);
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
            endRoom(roomId, now);
            updateParticipant(roomId, uid, P_LEFT, now);
            // 对端参与者一并置为已离开，否则其会一直被视为"通话中"（hasActiveRoom 判定为忙线）
            endPeerParticipants(roomId, uid, now);

            Long peerUid = peerOf(room, uid);
            JSONObject content = new JSONObject();
            content.put("roomId", roomId);
            pushRtcSignal(peerUid, uid, MessageType.rtcSignalHangup.getCode(), content.toJSONString());

            // 通话记录：接通后按时长记录（<3s 记未接通），未接通记 failed
            JSONObject record = new JSONObject();
            record.put("roomId", roomId);
            record.put("mediaType", parseMediaType(room.getRoomType()));
            if (room.getStatus() == ROOM_ACTIVE && room.getStartTime() != null) {
                long duration = Duration.between(room.getStartTime(), now).getSeconds();
                if (duration >= 3) {
                    record.put("event", "completed");
                    record.put("duration", duration);
                } else {
                    record.put("event", "failed");
                }
            } else {
                record.put("event", "failed");
            }
            // 通话记录只由挂断方发送（避免双方各发一条导致会话内重复）
            pushRecord(peerUid, uid, record);

            log.info("RTC 挂断, roomId={}, by={}", roomId, uid);
            return null;
        }, "操作过于频繁，请重试");
    }

    @Override
    public void cancel(Long uid, String roomId) {
        lockHelper.tryLock("rtc:room:" + roomId, 3, 10, () -> {
            RtcRoom room = rtcRoomMapper.selectById(roomId);
            // 仅主叫在呼叫中可取消
            if (room == null || room.getStatus() != ROOM_CALLING
                    || !String.valueOf(uid).equals(room.getHostUid())) {
                return null;
            }

            LocalDateTime now = LocalDateTime.now();
            endRoom(roomId, now);
            rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                    .eq(RtcParticipant::getRoomId, roomId)
                    .set(RtcParticipant::getStatus, P_LEFT)
                    .set(RtcParticipant::getLeaveTime, now));

            Long calleeUid = Long.parseLong(room.getChannelId());
            JSONObject content = new JSONObject();
            content.put("roomId", roomId);
            pushRtcSignal(calleeUid, uid, MessageType.rtcSignalCancel.getCode(), content.toJSONString());

            JSONObject record = new JSONObject();
            record.put("event", "canceled");
            record.put("roomId", roomId);
            record.put("mediaType", parseMediaType(room.getRoomType()));
            // 通话记录只由取消方发送（避免双方各发一条导致会话内重复）
            pushRecord(calleeUid, uid, record);

            log.info("RTC 取消, roomId={}, caller={}", roomId, uid);
            return null;
        }, "操作过于频繁，请重试");
    }

    @Override
    public List<IceServerVO> getIceServers() {
        List<IceServerVO> servers = rtcProperties.getIceServers();
        if (servers == null || servers.isEmpty()) {
            IceServerVO stun = new IceServerVO();
            stun.setUrls("stun:stun.l.google.com:19302");
            return List.of(stun);
        }
        return servers;
    }

    @Override
    public int expireStaleRooms() {
        LocalDateTime deadline = LocalDateTime.now().minusSeconds(rtcProperties.getCallTimeoutSeconds());
        List<RtcRoom> stale = rtcRoomMapper.selectList(new LambdaQueryWrapper<RtcRoom>()
                .eq(RtcRoom::getStatus, ROOM_CALLING)
                .lt(RtcRoom::getCreateTime, deadline));

        for (RtcRoom room : stale) {
            lockHelper.tryLock("rtc:room:" + room.getRoomId(), 3, 10, () -> {
                RtcRoom fresh = rtcRoomMapper.selectById(room.getRoomId());
                if (fresh == null || fresh.getStatus() != ROOM_CALLING) {
                    return null;
                }
                LocalDateTime now = LocalDateTime.now();
                endRoom(fresh.getRoomId(), now);
                rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                        .eq(RtcParticipant::getRoomId, fresh.getRoomId())
                        .set(RtcParticipant::getStatus, P_LEFT)
                        .set(RtcParticipant::getLeaveTime, now));

                Long callerUid = Long.parseLong(fresh.getHostUid());
                Long calleeUid = Long.parseLong(fresh.getChannelId());
                JSONObject content = new JSONObject();
                content.put("roomId", fresh.getRoomId());
                pushRtcSignal(calleeUid, callerUid, MessageType.rtcSignalCancel.getCode(), content.toJSONString());
                pushRtcSignal(callerUid, calleeUid, MessageType.rtcSignalCancel.getCode(), content.toJSONString());

                JSONObject record = new JSONObject();
                record.put("event", "missed");
                record.put("roomId", fresh.getRoomId());
                record.put("mediaType", parseMediaType(fresh.getRoomType()));
                // 通话记录只由主叫方发送（避免双方各发一条导致会话内重复）
                pushRecord(calleeUid, callerUid, record);

                log.info("RTC 呼叫超时清理, roomId={}", fresh.getRoomId());
                return null;
            }, "房间清理冲突，跳过");
        }
        return stale.size();
    }

    /**
     * 用户是否有活跃房间（呼叫中/通话中）
     * */
    private boolean hasActiveRoom(Long uid) {
        List<RtcParticipant> participants = rtcParticipantMapper.selectList(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getUId, uid)
                .in(RtcParticipant::getStatus, P_CALLING, P_JOINED));
        if (participants.isEmpty()) {
            return false;
        }
        // 排除已结束房间的残留参与者记录（异常退出/历史数据可能留下"呼叫中/已加入"）
        List<String> roomIds = participants.stream().map(RtcParticipant::getRoomId).distinct().toList();
        Long count = rtcRoomMapper.selectCount(new LambdaQueryWrapper<RtcRoom>()
                .in(RtcRoom::getRoomId, roomIds)
                .ne(RtcRoom::getStatus, ROOM_ENDED));
        return count != null && count > 0;
    }

    private RtcParticipant getParticipant(String roomId, Long uid) {
        return rtcParticipantMapper.selectOne(new LambdaQueryWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .eq(RtcParticipant::getUId, uid));
    }

    private RtcParticipant participant(String roomId, Long uid) {
        RtcParticipant p = new RtcParticipant();
        p.setRoomId(roomId);
        p.setUId(uid);
        p.setRole(0);
        p.setStatus(P_CALLING);
        return p;
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
     * <p>挂断/拒绝时若只更新发起方，对端参与者会残留为"呼叫中/已加入"，
     * 导致 hasActiveRoom 误判其长期忙线。</p>
     * */
    private void endPeerParticipants(String roomId, Long exceptUid, LocalDateTime now) {
        rtcParticipantMapper.update(null, new LambdaUpdateWrapper<RtcParticipant>()
                .eq(RtcParticipant::getRoomId, roomId)
                .ne(RtcParticipant::getUId, exceptUid)
                .set(RtcParticipant::getStatus, P_LEFT)
                .set(RtcParticipant::getLeaveTime, now));
    }

    private void endRoom(String roomId, LocalDateTime now) {
        rtcRoomMapper.update(null, new LambdaUpdateWrapper<RtcRoom>()
                .eq(RtcRoom::getRoomId, roomId)
                .set(RtcRoom::getStatus, ROOM_ENDED)
                .set(RtcRoom::getEndTime, now));
    }

    /**
     * 房间内对端用户：hostUid 存主叫、channelId 存被叫
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
     * 推送通话记录消息（type=210，持久化）。
     * content 为可直接展示的文本（如"语音通话 · 通话时长 03:25"），
     * 便于会话列表预览与消息气泡直接渲染。
     * */
    private void pushRecord(Long toUid, Long fromUid, JSONObject record) {
        pushRtcSignal(toUid, fromUid, MessageType.rtcRecord.getCode(), buildRecordText(record));
    }

    /**
     * 生成通话记录展示文本
     * */
    private String buildRecordText(JSONObject record) {
        boolean video = record.getInteger("mediaType") != null && record.getInteger("mediaType") == 2;
        String media = video ? "视频通话" : "语音通话";
        String event = record.getString("event");
        if (event == null) {
            return media + " · 未接通";
        }
        return switch (event) {
            case "completed" -> media + " · 通话时长 " + formatDuration(record.getLongValue("duration"));
            case "missed" -> media + " · 未接听";
            case "canceled" -> media + " · 已取消";
            case "rejected" -> media + " · 对方已拒绝";
            default -> media + " · 未接通";
        };
    }

    private String formatDuration(long seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
