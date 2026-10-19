package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 修改群内昵称
 * */
@Data
public class GroupNicknameDTO {

    /**
     * 群聊 Id
     * */
    @NotNull(message = "群聊Id不能为空")
    private Long groupId;

    /**
     * 新的群内昵称
     * */
    @NotBlank(message = "群昵称不能为空")
    @Length(max = 50, message = "群昵称不能超过50个字符")
    private String groupNickname;
}
