package com.chat.ai.provider.core;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * AI 对话选项
 */
@Data
@Builder
public class AiChatOptions {

    /**
     * 模型名称
     */
    private String model;

    /**
     * 温度
     */
    private Double temperature;

    /**
     * 最大生成 token 数
     */
    private Integer maxTokens;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 关联知识库 ID
     */
    private List<Long> knowledgeBaseIds;

    /**
     * 是否启用工具调用
     */
    private Boolean enableTools;
}
