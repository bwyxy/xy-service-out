package com.xy.blog.pojo.dto;

import lombok.Data;

@Data
public class PhotoDTO {

    /** 页码 */
    private Integer pageNum = 1;

    /** 数量 */
    private Integer pageSize = 6;

    /** 分类 Id */
    private Long categoryId;
}
