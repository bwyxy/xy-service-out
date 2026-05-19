package com.todo.blog.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDTO {

    private int pageNum;

    private int pageSize;

    private Long categoryId;

    private Long tagId;

    private String title;
}
