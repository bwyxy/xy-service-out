package com.chat.ai.provider.dashscope;

import com.chat.ai.provider.core.AiChatOptions;
import com.chat.ai.provider.core.AiModelResponse;
import com.chat.ai.provider.core.AiStreamChunk;
import com.chat.ai.provider.core.ModelProvider;
import com.chat.ai.provider.openai.OpenAiModelProvider;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Function;

/**
 * 阿里云百炼模型提供者实现
 * <p>
 * 百炼提供 OpenAI 兼容接口，当前实现复用 OpenAiModelProvider 的调用逻辑，
 * 便于后续接入 DashScope Java SDK 的原生高级能力。
 */
@Component
public class DashscopeModelProvider implements ModelProvider {

    public static final String CODE = "dashscope";

    private final OpenAiModelProvider openAiDelegate;

    public DashscopeModelProvider(OpenAiModelProvider openAiDelegate) {
        this.openAiDelegate = openAiDelegate;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    @Override
    public AiModelResponse chat(List<Message> messages, AiChatOptions options) {
        return openAiDelegate.chat(messages, options);
    }

    @Override
    public Flux<AiStreamChunk> streamChat(List<Message> messages, AiChatOptions options) {
        return openAiDelegate.streamChat(messages, options);
    }

    @Override
    public void registerTool(String name, String description, String schema, Function<String, String> executor) {
        openAiDelegate.registerTool(name, description, schema, executor);
    }
}
