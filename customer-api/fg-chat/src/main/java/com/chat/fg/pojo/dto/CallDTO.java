package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 发起通话请求 DTO
 */
@Data
public class CallDTO {

    /**
     * 被叫用户 ID
     */
    @NotNull(message = "被叫用户不能为空")
    private Long calleeUid;

    /**
     * 媒体类型 1-语音 2-视频
     */
    @NotNull(message = "媒体类型不能为空 1-语音 2-视频")
    private Integer mediaType;
}
