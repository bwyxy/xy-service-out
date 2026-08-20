package com.chat.ai.provider.core;

import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/**
 * AI 模型提供者抽象
 */
public interface ModelProvider {

    /**
     * 提供者编码
     */
    String getCode();

    /**
     * 同步对话
     */
    AiModelResponse chat(List<Message> messages, AiChatOptions options);

    /**
     * 流式对话
     */
    Flux<AiStreamChunk> streamChat(List<Message> messages, AiChatOptions options);

    /**
     * 注册工具函数
     */
    void registerTool(String name, String description, String schema, Function<String, String> executor);

    /**
     * 是否支持工具调用
     */
    default boolean supportTools() {
        return true;
    }
}
