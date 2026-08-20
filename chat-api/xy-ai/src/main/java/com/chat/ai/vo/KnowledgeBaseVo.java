package com.chat.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
@Data
public class KnowledgeBaseVo {

    private Long id;

    private String name;

    private String description;

    private String embeddingModel;

    private String providerCode;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
