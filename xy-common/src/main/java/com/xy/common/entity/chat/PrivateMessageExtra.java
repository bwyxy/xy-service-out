package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 消息扩展信息 t_private_message_extra
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_private_message_extra")
public class PrivateMessageExtra {

    /**
     * id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 频道Id
     * */
    private String channelId;

    /**
     * 消息 seq
     * */
    private Long messageSeq;

    /**
     * 发送者 uId
     * */
    private Long fromUid;

    /**
     * 接收者（单聊）
     * */
    private Long toUid;

    /**
     * 是否已撤回
     * */
    private int isRecall;

    /**
     * 撤回时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime recallTime;

    /**
     * 已读人数
     * */
    private Integer readCount;

    /**
     * 扩展字段
     * */
    private String extra;

    /**
     * 创建时间
     * */
    @TableField(fill = FieldFill.INSERT, updateStrategy = FieldStrategy.NEVER)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
