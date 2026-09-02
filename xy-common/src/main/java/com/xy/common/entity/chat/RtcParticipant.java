package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * RTC房间参与者 t_rtc_participant
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_rtc_participant")
public class RtcParticipant {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 房间ID
     * */
    private String roomId;

    /**
     * 用户ID
     * */
    private Long uId;

    /**
     * 角色 0-普通 1-主持人
     * */
    private int role;

    /**
     * 状态 0-呼叫中 1-已加入 2-已离开 3-拒绝 4-忙线
     * */
    private int status;

    /**
     * 加入时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime joinTime;

    /**
     * 离开时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime leaveTime;

    /**
     * 是否静音
     * */
    private int audioMuted;

    /**
     * 是否关闭视频
     * */
    private int videoMuted;

    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
