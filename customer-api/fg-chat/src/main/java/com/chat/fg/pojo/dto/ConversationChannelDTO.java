package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会话频道请求 DTO（删除聊天记录 / 删除会话）
 */
@Data
public class ConversationChannelDTO {

    /**
     * 频道 ID
     */
    @NotBlank(message = "频道 Id 不能为空")
    private String channelId;

    /**
     * 频道类型 (1=个人频道, 2=群组频道)
     */
    @NotNull(message = "频道类型不能为空 1-个人频道 2-群组频道")
    private Integer channelType;
}
