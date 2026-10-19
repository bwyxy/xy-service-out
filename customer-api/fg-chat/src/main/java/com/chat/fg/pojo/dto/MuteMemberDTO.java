package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 群成员禁言
 * */
@Data
public class MuteMemberDTO {

    /**
     * 群聊 Id
     * */
    @NotNull(message = "群聊Id不能为空")
    private Long groupId;

    /**
     * 被禁言的成员 Id
     * */
    @NotNull(message = "成员Id不能为空")
    private Long uid;

    /**
     * 禁言时长（分钟），为空或小于等于 0 表示解除禁言
     * */
    private Integer muteMinutes;
}
