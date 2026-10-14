package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 房间操作请求 DTO（接听/拒绝/挂断/取消）
 */
@Data
public class RoomDTO {

    /**
     * 房间 ID
     */
    @NotBlank(message = "房间 Id 不能为空")
    private String roomId;
}
