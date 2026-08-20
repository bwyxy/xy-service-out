package com.chat.fg.pojo.dto;

import lombok.Data;

/**
 * AI 对话响应 DTO
 */
@Data
public class AiChatResponse {

    /**
     * 会话 ID
     */
    private Long conversationId;

    /**
     * 消息 ID
     */
    private Long messageId;

    /**
     * AI 回复内容
     */
    private String content;

    /**
     * 使用的模型
     */
    private String model;

    /**
     * 是否流式结束标记
     */
    private Boolean done;

    /**
     * 错误信息
     */
    private String errorMsg;
}
