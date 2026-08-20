package com.chat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.ai.entity.AiMessage;
import com.chat.ai.enums.MessageRole;
import com.chat.ai.enums.MessageStatus;
import com.chat.ai.mapper.AiMessageMapper;
import com.chat.ai.provider.core.AiUsage;
import com.chat.ai.service.AiMessageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 消息服务实现
 */
@Slf4j
@Service
public class AiMessageServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage>
        implements AiMessageService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AiMessage saveUserMessage(Long conversationId, String content) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(MessageRole.USER.getCode());
        message.setContent(content);
        message.setStatus(MessageStatus.COMPLETED.getCode());
        message.setCreateTime(LocalDateTime.now());
        save(message);
        return message;
    }

    @Override
    public AiMessage saveAssistantMessage(Long conversationId, String model) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(MessageRole.ASSISTANT.getCode());
        message.setContent("");
        message.setModel(model);
        message.setStatus(MessageStatus.GENERATING.getCode());
        message.setCreateTime(LocalDateTime.now());
        save(message);
        return message;
    }

    @Override
    public boolean completeAssistantMessage(Long messageId, String content, AiUsage usage) {
        AiMessage message = getById(messageId);
        if (message == null) {
            return false;
        }
        message.setContent(content);
        message.setStatus(MessageStatus.COMPLETED.getCode());
        if (usage != null) {
            try {
                message.setUsageJson(objectMapper.writeValueAsString(usage));
            } catch (JsonProcessingException e) {
                log.warn("序列化 Usage 失败", e);
            }
        }
        return updateById(message);
    }

    @Override
    public boolean failMessage(Long messageId, String errorMsg) {
        AiMessage message = getById(messageId);
        if (message == null) {
            return false;
        }
        message.setStatus(MessageStatus.FAILED.getCode());
        message.setErrorMsg(errorMsg);
        return updateById(message);
    }

    @Override
    public List<AiMessage> listByConversation(Long conversationId) {
        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getCreateTime);
        return list(wrapper);
    }
}
