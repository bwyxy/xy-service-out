package com.chat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模型提供者配置
 */
@Data
@TableName("ai_model_provider")
public class AiModelProvider {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String type;

    private String endpoint;

    private String apiKey;

    private String model;

    private Integer priority;

    private Integer enabled;

    private String configJson;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
