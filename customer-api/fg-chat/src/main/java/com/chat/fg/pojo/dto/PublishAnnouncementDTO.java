package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

/**
 * 发布群公告
 * */
@Data
public class PublishAnnouncementDTO {

    /**
     * 群聊 Id
     * */
    @NotNull(message = "群聊Id不能为空")
    private Long groupId;

    /**
     * 公告标题
     * */
    @Length(max = 100, message = "公告标题不能超过100个字符")
    private String title;

    /**
     * 公告内容
     * */
    @NotBlank(message = "公告内容不能为空")
    private String content;

    /**
     * 是否置顶 0-否 1-是
     * */
    private Integer isPinned;
}
