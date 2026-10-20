package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 发起通话请求 DTO
 *
 * <p>单聊：传 calleeUid；群聊：传 channelId（g_{groupId}）+ channelType=2 + inviteeUids。</p>
 */
@Data
public class CallDTO {

    /**
     * 被叫用户 ID（单聊必填）
     */
    private Long calleeUid;

    /**
     * 群聊频道 ID，形如 g_9
     */
    private String channelId;

    /**
     * 频道类型 1-单聊 2-群聊，默认 1
     */
    private Integer channelType;

    /**
     * 邀请的群成员用户 ID（群聊必填，不含自己）
     */
    private List<Long> inviteeUids;

    /**
     * 媒体类型 1-语音 2-视频
     */
    @NotNull(message = "媒体类型不能为空 1-语音 2-视频")
    private Integer mediaType;
}
