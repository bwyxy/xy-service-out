package com.xy.common.dto.ai;

import lombok.Data;

/**
 * 知识库分页查询
 */
@Data
public class KnowledgeBasePageQuery {

    private String keyword;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
