package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 消息撤回
 * */
@Data
public class RecallMessageDTO {

    @NotNull(message = "频道Id不能为空")
    private String channelId;

    @NotNull(message = "频道类型不能为空")
    private Integer channelType;

    /**
     * 原消息序号（悟空 IM 分配）
     * */
    @NotNull(message = "消息序号不能为空")
    private Long messageSeq;

    /**
     * 原消息客户端消息号（服务端按序号匹配不到时的兜底）
     * */
    private String clientMsgNo;
}
