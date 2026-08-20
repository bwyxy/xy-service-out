package com.chat.ai.service;

import com.chat.ai.dto.ChatRequest;
import com.chat.ai.dto.ChatResponse;
import com.chat.ai.provider.core.AiStreamChunk;
import reactor.core.publisher.Flux;

/**
 * AI 对话服务
 */
public interface AiChatService {

    /**
     * 同步对话
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式对话
     */
    Flux<AiStreamChunk> streamChat(ChatRequest request);
}
