package com.chat.fg.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 通话房间详情 VO（页面刷新后重新入房使用）
 */
@Data
public class RoomDetailVO {

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
     * 发起人 uid
     */
    private String hostUid;

    /**
     * 房间状态 0-呼叫中 1-通话中 2-已结束
     */
    private Integer status;

    /**
     * 房间人数上限
     */
    private Integer maxParticipants;

    /**
     * 下发给前端的 LiveKit 地址
     */
    private String livekitUrl;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 房间成员
     */
    private List<RtcMemberVO> participants = new ArrayList<>();
}
