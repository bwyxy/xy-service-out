package com.xy.common.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模型提供者视图对象
 */
@Data
public class AiModelProviderVo {

    private Long id;

    private String name;

    private String code;

    private String type;

    private String endpoint;

    private String model;

    private Integer priority;

    private Integer enabled;

    private String configJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
