package com.chat.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.ai.entity.AiMessage;
import com.chat.ai.enums.MessageRole;
import com.chat.ai.provider.core.AiUsage;

import java.util.List;

/**
 * AI 消息服务
 */
public interface AiMessageService extends IService<AiMessage> {

    /**
     * 保存用户消息
     */
    AiMessage saveUserMessage(Long conversationId, String content);

    /**
     * 保存助手消息（生成中）
     */
    AiMessage saveAssistantMessage(Long conversationId, String model);

    /**
     * 完成助手消息
     */
    boolean completeAssistantMessage(Long messageId, String content, AiUsage usage);

    /**
     * 标记消息失败
     */
    boolean failMessage(Long messageId, String errorMsg);

    /**
     * 查询会话历史消息
     */
    List<AiMessage> listByConversation(Long conversationId);
}
