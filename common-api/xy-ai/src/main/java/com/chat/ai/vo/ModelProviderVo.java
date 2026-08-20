package com.chat.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 模型提供者视图对象
 */
@Data
public class ModelProviderVo {

    private Long id;

    private String name;

    private String code;

    private String type;

    private String endpoint;

    private String model;

    private Integer priority;

    private Integer enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
