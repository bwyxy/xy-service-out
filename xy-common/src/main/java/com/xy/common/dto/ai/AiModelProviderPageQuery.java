package com.xy.common.dto.ai;

import lombok.Data;

/**
 * AI 模型提供者分页查询
 */
@Data
public class AiModelProviderPageQuery {

    private String keyword;

    private Integer enabled;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
