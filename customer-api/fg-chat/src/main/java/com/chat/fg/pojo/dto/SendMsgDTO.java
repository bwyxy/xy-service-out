package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发送消息请求 DTO
 */
@Data
public class SendMsgDTO {

    /**
     * 目标频道 ID
     */
    @NotBlank(message = "频道 Id 不能为空")
    private String channelId;

    /**
     * 频道类型 (1=个人频道, 2=群组频道)
     */
    @NotNull(message = "频道类型不能为空 1-个人频道 2-群组频道")
    private Integer channelType;

    /**
     * 消息类型 (1=文本, 2=图片, 3=文件)
     */
    @NotNull(message = "消息类型不能为空")
    private Integer type;

    /**
     * 消息内容（文本内容或图片/文件 URL）
     */
    @NotBlank(message = "消息内容不能为空")
    private String content;

    /**
     * 客户端消息编号，用于去重
     */
    private String clientMsgNo;
}
