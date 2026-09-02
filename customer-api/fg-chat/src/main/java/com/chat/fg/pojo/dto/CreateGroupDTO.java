package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * 创建群聊
 * */
@Data
public class CreateGroupDTO {

    /**
     * 群名称
     * */
    @NotBlank(message = "群名称不能为空")
    @NotNull(message = "群名称不能为空")
    @Length(min = 2, message = "群名称不能低于两个字符")
    private String groupName;

    /**
     * 群主 Id
     * */
    @NotBlank(message = "群主ID不能为空")
    @NotNull(message = "群主ID不能为空")
    private Long uId;

    /**
     * 群成员 ID
     * */
    @NotEmpty(message = "群成员ID不能为空")
    @Size(min = 2, message = "群成员ID列表不能低于两个成员")
    private List<Long> memberIds;
}
