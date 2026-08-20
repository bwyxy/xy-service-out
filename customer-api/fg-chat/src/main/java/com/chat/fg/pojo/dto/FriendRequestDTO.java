package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送好友请求 DTO
 * */
@Data
public class FriendRequestDTO {

    /**
     * 目标用户ID
     * */
    @NotNull(message = "目标用户ID不能为空")
    private Long toUid;

    /**
     * 申请备注消息
     * */
    @Size(max = 255, message = "申请备注不能超过255个字符")
    private String applyMsg;
}
