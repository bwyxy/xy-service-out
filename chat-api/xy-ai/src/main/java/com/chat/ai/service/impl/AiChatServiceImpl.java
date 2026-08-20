package com.chat.ai.service.impl;

import com.chat.ai.dto.ChatRequest;
import com.chat.ai.dto.ChatResponse;
import com.chat.ai.entity.AiConversation;
import com.chat.ai.entity.AiMessage;
import com.chat.ai.enums.MessageRole;
import com.chat.ai.enums.UserType;
import com.chat.ai.exception.AiException;
import com.chat.ai.provider.core.*;
import com.chat.ai.service.AiChatService;
import com.chat.ai.service.AiConversationService;
import com.chat.ai.service.AiMessageService;
import com.chat.ai.service.AiQuotaService;
import com.chat.ai.tool.core.ToolRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话服务实现
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private final ModelProviderFactory providerFactory;

    private final AiConversationService conversationService;

    private final AiMessageService messageService;

    private final AiQuotaService quotaService;

    private final ToolRegistry toolRegistry;

    public AiChatServiceImpl(ModelProviderFactory providerFactory,
                             AiConversationService conversationService,
                             AiMessageService messageService,
                             AiQuotaService quotaService,
                             ToolRegistry toolRegistry) {
        this.providerFactory = providerFactory;
        this.conversationService = conversationService;
        this.messageService = messageService;
        this.quotaService = quotaService;
        this.toolRegistry = toolRegistry;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        quotaService.consumeQuota(request.getUserId(), request.getUserType());

        AiConversation conversation = resolveConversation(request);
        messageService.saveUserMessage(conversation.getId(), request.getMessage());

        AiChatOptions options = buildOptions(request, conversation);
        List<Message> messages = buildMessages(conversation, request, options);

        AiMessage assistantMessage = messageService.saveAssistantMessage(conversation.getId(),
                options.getModel() != null ? options.getModel() : conversation.getModel());

        try {
            ModelProvider provider = providerFactory.getProvider(conversation.getProviderCode());
            AiModelResponse modelResponse = provider.chat(messages, options);
            messageService.completeAssistantMessage(assistantMessage.getId(),
                    modelResponse.getContent(), modelResponse.getUsage());

            return ChatResponse.builder()
                    .conversationId(conversation.getId())
                    .messageId(assistantMessage.getId())
                    .content(modelResponse.getContent())
                    .model(modelResponse.getModel())
                    .build();
        } catch (Exception e) {
            log.warn("AI 对话失败", e);
            messageService.failMessage(assistantMessage.getId(), e.getMessage());
            throw new AiException("AI 对话失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<AiStreamChunk> streamChat(ChatRequest request) {
        quotaService.consumeQuota(request.getUserId(), request.getUserType());

        AiConversation conversation = resolveConversation(request);
        messageService.saveUserMessage(conversation.getId(), request.getMessage());

        AiChatOptions options = buildOptions(request, conversation);
        List<Message> messages = buildMessages(conversation, request, options);

        AiMessage assistantMessage = messageService.saveAssistantMessage(conversation.getId(),
                options.getModel() != null ? options.getModel() : conversation.getModel());

        ModelProvider provider;
        try {
            provider = providerFactory.getProvider(conversation.getProviderCode());
        } catch (Exception e) {
            return Flux.just(AiStreamChunk.builder()
                    .errorMsg(e.getMessage())
                    .done(true)
                    .build());
        }

        StringBuilder contentBuffer = new StringBuilder();
        return provider.streamChat(messages, options)
                .doOnNext(chunk -> {
                    if (chunk.getContent() != null) {
                        contentBuffer.append(chunk.getContent());
                    }
                })
                .doOnComplete(() -> {
                    messageService.completeAssistantMessage(assistantMessage.getId(),
                            contentBuffer.toString(), null);
                })
                .doOnError(error -> {
                    log.warn("AI 流式对话失败", error);
                    messageService.failMessage(assistantMessage.getId(), error.getMessage());
                });
    }

    private AiConversation resolveConversation(ChatRequest request) {
        if (request.getConversationId() != null) {
            AiConversation conversation = conversationService.getById(request.getConversationId());
            if (conversation != null) {
                return conversation;
            }
        }
        String providerCode = request.getProviderCode() != null ? request.getProviderCode()
                : providerFactory.hasProvider("openai") ? "openai" : null;
        if (providerCode == null) {
            throw new AiException("未配置默认模型提供者");
        }
        return conversationService.createConversation(
                request.getUserId(),
                UserType.of(request.getUserType()).getCode(),
                request.getMessage().length() > 20 ? request.getMessage().substring(0, 20) + "..." : request.getMessage(),
                providerCode,
                request.getModel(),
                request.getSystemPrompt()
        );
    }

    private AiChatOptions buildOptions(ChatRequest request, AiConversation conversation) {
        return AiChatOptions.builder()
                .model(request.getModel() != null ? request.getModel() : conversation.getModel())
                .systemPrompt(request.getSystemPrompt() != null ? request.getSystemPrompt() : conversation.getSystemPrompt())
                .knowledgeBaseIds(request.getKnowledgeBaseIds())
                .enableTools(request.getEnableTools())
                .build();
    }

    private List<Message> buildMessages(AiConversation conversation, ChatRequest request, AiChatOptions options) {
        List<Message> messages = new ArrayList<>();

        String systemPrompt = options.getSystemPrompt();
        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new SystemMessage(systemPrompt));
        }

        // RAG 上下文增强
        if (options.getKnowledgeBaseIds() != null && !options.getKnowledgeBaseIds().isEmpty()) {
            StringBuilder context = new StringBuilder("请参考以下知识库内容回答问题：\n");
            // 此处通过依赖注入获取 KnowledgeBaseService 会循环依赖，简化为空实现；
            // 实际可在 Controller 层拼接或引入检索代理。
            messages.add(new SystemMessage(context.toString()));
        }

        List<AiMessage> history = messageService.listByConversation(conversation.getId());
        for (AiMessage msg : history) {
            String role = msg.getRole();
            if (MessageRole.USER.getCode().equals(role)) {
                messages.add(new UserMessage(msg.getContent()));
            } else if (MessageRole.ASSISTANT.getCode().equals(role)) {
                messages.add(new AssistantMessage(msg.getContent()));
            } else if (MessageRole.SYSTEM.getCode().equals(role)) {
                messages.add(new SystemMessage(msg.getContent()));
            }
        }

        // 最后追加当前用户消息（历史可能已包含，但新会话没有）
        boolean exists = history.stream()
                .anyMatch(m -> MessageRole.USER.getCode().equals(m.getRole())
                        && request.getMessage().equals(m.getContent()));
        if (!exists) {
            messages.add(new UserMessage(request.getMessage()));
        }

        return messages;
    }
}
