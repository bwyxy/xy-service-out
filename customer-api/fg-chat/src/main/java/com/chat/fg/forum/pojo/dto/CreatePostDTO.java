package com.chat.fg.forum.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 发布帖子请求 DTO
 */
@Data
public class CreatePostDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 板块 ID
     */
    @NotNull(message = "板块不能为空")
    private Long boardId;

    /**
     * 标题
     */
    @NotBlank(message = "标题不能为空")
    @Size(min = 5, max = 100, message = "标题长度需在 5-100 字符之间")
    private String title;

    /**
     * 正文
     */
    @NotBlank(message = "正文不能为空")
    @Size(max = 50000, message = "正文长度不能超过 50000 字符")
    private String content;

    /**
     * 内容类型：1-Markdown 2-富文本
     */
    private Integer contentType = 1;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 可见性：1-公开 2-好友可见 3-私密
     */
    private Integer visibility = 1;

    /**
     * 话题名称列表
     */
    private List<String> topicNames;
}
