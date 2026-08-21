package com.chat.fg.pojo.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话请求 DTO
 */
@Data
public class AiChatRequest {

    /**
     * 会话 ID，新建会话可空
     */
    private Long conversationId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 用户类型：CLIENT / ADMIN
     */
    private String userType;

    /**
     * 用户输入消息
     */
    private String message;

    /**
     * 指定模型提供者编码，可空
     */
    private String providerCode;

    /**
     * 指定模型，可空
     */
    private String model;

    /**
     * 系统提示词，可空
     */
    private String systemPrompt;

    /**
     * 关联知识库 ID 列表
     */
    private List<Long> knowledgeBaseIds;

    /**
     * 是否启用 Agent 工具调用
     */
    private Boolean enableTools;
}
