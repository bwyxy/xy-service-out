package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 编辑群聊信息
 * */
@Data
public class EditGroupDTO {

    @NotNull(message = "群聊Id不能为空")
    private Long groupId;

    @Length(min = 2, message = "群名称不能低于两个字符")
    private String name;

    private String avatar;

    /**
     * 是否禁言
     * */
    private Integer isBan;

    /**
     * 新成员是否可见历史消息
     * */
    private Integer historyVisible;
}
