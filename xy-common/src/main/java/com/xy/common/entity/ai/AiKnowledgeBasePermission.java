package com.xy.common.entity.ai;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 知识库权限
 */
@Data
@TableName("ai_knowledge_base_permission")
public class AiKnowledgeBasePermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String targetType;

    private Long targetId;

    private String permission;

    private LocalDateTime createTime;
}
