package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * RTC房间 t_rtc_room
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_rtc_room")
public class RtcRoom {

    /**
     * 房间Id
     * */
    @TableId(value = "room_id")
    private String roomId;

    /**
     * 房间类型 1-语音 2-视频 3-会议
     * */
    private String roomType;

    /**
     * 房主/主持人
     * */
    private String hostUid;

    /**
     * 关联悟空IM频道
     * */
    private String channelId;

    /**
     * 频道类型 1-单聊 2-群聊
     * */
    private Integer channelType;

    /**
     * 最大人数
     * */
    private int maxParticipants;

    /**
     * 状态 0-呼叫中 1-通话中 2-已结束
     * */
    private int status;

    /**
     * 开始时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 结束时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime endTime;

    /**
     * 分配的SFU节点
     * */
    private String sfuNode;

    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
