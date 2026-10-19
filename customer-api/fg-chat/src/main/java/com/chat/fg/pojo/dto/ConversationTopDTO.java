package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 会话置顶请求 DTO
 */
@Data
public class ConversationTopDTO {

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

    /**
     * 是否置顶 (1=置顶, 0=取消置顶)
     */
    @NotNull(message = "置顶状态不能为空")
    private Integer top;
}
