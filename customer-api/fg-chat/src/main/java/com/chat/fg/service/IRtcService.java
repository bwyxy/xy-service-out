package com.chat.fg.service;

import com.chat.fg.pojo.dto.CallDTO;
import com.chat.fg.pojo.dto.InviteDTO;
import com.chat.fg.pojo.vo.CallVO;
import com.chat.fg.pojo.vo.InviteVO;
import com.chat.fg.pojo.vo.RoomDetailVO;

/**
 * 音视频通话 逻辑层
 *
 * <p>呼叫控制信令走悟空 IM 个人频道（200-205），媒体面统一由 LiveKit SFU 承载。</p>
 * */
public interface IRtcService {

    /**
     * 发起通话：创建房间（单聊/群聊）并向被邀请人推送邀请信令
     *
     * @param callerUid 主叫用户 ID
     * @param dto 呼叫参数（单聊传 calleeUid；群聊传 channelId + inviteeUids）
     * @return 房间信息与当前用户的 LiveKit 接入 token
     * */
    CallVO call(Long callerUid, CallDTO dto);

    /**
     * 接听/重新接入：参与者置为已加入，满足条件时房间进入通话中，返回接入 token
     * */
    CallVO accept(Long uid, String roomId);

    /**
     * 拒绝：单聊结束房间并通知主叫，群聊仅本人退出邀请
     * */
    void reject(Long uid, String roomId);

    /**
     * 挂断/离开：单聊结束房间；群聊仅本人离开，无人时结束房间
     * */
    void hangup(Long uid, String roomId);

    /**
     * 主叫取消（仅呼叫中）：结束房间并通知全部参与者
     * */
    void cancel(Long uid, String roomId);

    /**
     * 群通话中追加邀请成员
     * */
    InviteVO invite(Long uid, InviteDTO dto);

    /**
     * 结束整场通话（发起人或群主/管理员），仅群聊可用
     * */
    void end(Long uid, String roomId);

    /**
     * 重新签发接入 token（断线重连/页面刷新后重新入房）
     * */
    CallVO token(Long uid, String roomId);

    /**
     * 查询房间详情
     * */
    RoomDetailVO roomDetail(Long uid, String roomId);

    /**
     * LiveKit SFU 是否可用（健康检查）
     * */
    boolean isLivekitUp();

    /**
     * 清理超时/过期房间（供定时任务调用）
     *
     * @return 本次清理的房间数
     * */
    int expireStaleRooms();
}
