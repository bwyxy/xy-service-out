package com.todo.blog.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentDTO {

    /**
     * 文章 Id
     * */
    private Long articleId;

    /**
     * 评论类型
     * */
    private String commentType;
}
