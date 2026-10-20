package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 群通话中邀请成员请求 DTO
 */
@Data
public class InviteDTO {

    /**
     * 房间 ID
     */
    @NotBlank(message = "房间 Id 不能为空")
    private String roomId;

    /**
     * 被邀请的用户 ID 列表
     */
    @NotEmpty(message = "请选择要邀请的成员")
    private List<Long> inviteeUids;
}
