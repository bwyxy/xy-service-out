package com.chat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 会话
 */
@Data
@TableName("ai_conversation")
public class AiConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userType;

    private String title;

    private String providerCode;

    private String model;

    private String systemPrompt;

    private String paramsJson;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
