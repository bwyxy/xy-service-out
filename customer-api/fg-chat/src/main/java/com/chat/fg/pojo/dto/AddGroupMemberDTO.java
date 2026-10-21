package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 添加群成员
 * */
@Data
public class AddGroupMemberDTO {

    /**
     * 群聊 Id
     * */
    @NotNull(message = "群聊Id不能为空")
    private Long groupId;

    /**
     * 待添加的成员 Id
     * */
    @NotEmpty(message = "请选择要添加的成员")
    private List<Long> memberIds;
}
