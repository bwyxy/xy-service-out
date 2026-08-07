package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 处理好友请求 DTO
 * */
@Data
public class HandleFriendRequestDTO {

    /**
     * 好友请求ID
     * */
    @NotNull(message = "请求ID不能为空")
    private Long requestId;

    /**
     * 处理动作：1-同意 2-拒绝
     * */
    @NotNull(message = "处理动作不能为空")
    private Integer action;

    /**
     * 回复消息
     * */
    @Size(max = 255, message = "回复消息不能超过255个字符")
    private String replyMsg;
}
