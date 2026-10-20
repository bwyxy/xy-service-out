package com.chat.fg.pojo.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 通话会话信息 VO
 *
 * <p>发起、接听、重连刷新三个接口返回同一结构：
 * 前端拿到 livekitUrl + token 后即可接入 LiveKit 房间。</p>
 */
@Data
public class CallVO {

    /**
     * 房间 ID（同时是 LiveKit roomName）
     */
    private String roomId;

    /**
     * 媒体类型 1-语音 2-视频
     */
    private Integer mediaType;

    /**
     * 频道类型 1-单聊 2-群聊
     */
    private Integer channelType;

    /**
     * 频道 ID（单聊=对端 uid，群聊=g_{groupId}）
     */
    private String channelId;

    /**
     * 下发给前端的 LiveKit 地址
     */
    private String livekitUrl;

    /**
     * 当前用户接入 LiveKit 的 token
     */
    private String token;

    /**
     * 房间人数上限
     */
    private Integer maxParticipants;

    /**
     * 房间成员
     */
    private List<RtcMemberVO> participants = new ArrayList<>();
}
