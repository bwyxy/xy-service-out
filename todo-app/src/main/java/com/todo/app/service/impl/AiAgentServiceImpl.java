package com.todo.app.service.impl;

import com.todo.app.pojo.dto.AiChatRequest;
import com.todo.app.pojo.vo.AiChatResponse;
import com.todo.app.service.IAiAgentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Slf4j
@Service
public class AiAgentServiceImpl implements IAiAgentService {

    private static final String SYSTEM_PROMPT = """
            你是"忆待办"的AI智能助手，一个友好、专业的个人助理。
            
            你的职责：
            1. 帮助用户管理待办事项，提供时间管理和任务规划建议
            2. 回答用户关于博客、文章的问题
            3. 提供生活、学习、工作方面的建议
            4. 进行日常闲聊，保持友好亲切的语气
            
            回答要求：
            - 使用中文回答
            - 回答简洁明了，重点突出
            - 如果用户询问待办相关功能，引导用户使用小程序的相关功能
            - 保持积极、鼓励的态度
            """;

    private final ChatClient chatClient;

    public AiAgentServiceImpl(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @Override
    public AiChatResponse chat(AiChatRequest request) {
        String conversationId = getOrCreateConversationId(request);

        ChatResponse response = chatClient
                .prompt()
                .user(request.getMessage())
                .call()
                .chatResponse();

        String content = response.getResult().getOutput().getText();

        long promptTokens = 0;
        long completionTokens = 0;
        if (response.getMetadata() != null && response.getMetadata().getUsage() != null) {
            Integer pt = response.getMetadata().getUsage().getPromptTokens();
            Integer ct = response.getMetadata().getUsage().getCompletionTokens();
            promptTokens = pt != null ? pt : 0;
            completionTokens = ct != null ? ct : 0;
        }

        return AiChatResponse.builder()
                .content(content)
                .conversationId(conversationId)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .build();
    }

    @Override
    public Flux<String> chatStream(AiChatRequest request) {
        return chatClient
                .prompt()
                .user(request.getMessage())
                .stream()
                .content();
    }

    private String getOrCreateConversationId(AiChatRequest request) {
        if (request.getConversationId() != null && !request.getConversationId().isEmpty()) {
            return request.getConversationId();
        }
        return UUID.randomUUID().toString();
    }
}
