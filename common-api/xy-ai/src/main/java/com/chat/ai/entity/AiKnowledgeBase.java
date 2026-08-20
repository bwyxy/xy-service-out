package com.chat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库
 */
@Data
@TableName("ai_knowledge_base")
public class AiKnowledgeBase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String description;

    private String embeddingModel;

    private String providerCode;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
