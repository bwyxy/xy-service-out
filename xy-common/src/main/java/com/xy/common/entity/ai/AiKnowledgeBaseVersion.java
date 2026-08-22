package com.xy.common.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库版本
 */
@Data
@TableName("ai_knowledge_base_version")
public class AiKnowledgeBaseVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String version;

    private String description;

    private Integer docCount;

    private Integer status;

    private LocalDateTime publishedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
