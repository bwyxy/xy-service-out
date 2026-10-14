package com.chat.fg.service;

import com.chat.fg.pojo.dto.CallDTO;
import com.chat.fg.pojo.vo.CallVO;
import com.chat.fg.pojo.vo.IceServerVO;

import java.util.List;

/**
 * 音视频通话 逻辑层
 * */
public interface IRtcService {

    /**
     * 发起通话：创建房间并向被叫推送邀请信令
     *
     * @param callerUid 主叫用户 ID
     * @param dto 呼叫参数（被叫 + 媒体类型）
     * @return 房间信息
     * */
    CallVO call(Long callerUid, CallDTO dto);

    /**
     * 被叫接听：房间进入通话中并通知主叫
     * */
    void accept(Long uid, String roomId);

    /**
     * 被叫拒绝：结束房间、通知主叫、双端落通话记录
     * */
    void reject(Long uid, String roomId);

    /**
     * 挂断（任一方）：结束房间、通知对方、双端落通话记录（含时长）
     * */
    void hangup(Long uid, String roomId);

    /**
     * 主叫取消（仅呼叫中）：结束房间、通知被叫停铃、双端落通话记录
     * */
    void cancel(Long uid, String roomId);

    /**
     * 获取 ICE 服务器配置（STUN/TURN）
     * */
    List<IceServerVO> getIceServers();

    /**
     * 清理超时未接听的房间（供定时任务调用）
     *
     * @return 本次清理的房间数
     * */
    int expireStaleRooms();
}
