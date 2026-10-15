package com.xy.common.dto.blog;

import lombok.Data;

/**
 * 分发运营数据查询 DTO
 * */
@Data
public class DispatchStatQueryDTO {

    /** 平台编码，可选 */
    private String platformCode;

    /** 文章 Id，可选 */
    private Long articleId;

    /** 关键词（按文章标题模糊匹配），可选 */
    private String keyword;

    private int pageNum = 1;

    private int pageSize = 10;
}
