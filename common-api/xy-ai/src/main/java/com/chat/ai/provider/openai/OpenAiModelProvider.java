package com.chat.ai.provider.openai;

import com.xy.common.entity.ai.AiModelProvider;
import com.chat.ai.provider.core.*;
import com.chat.ai.service.AiProviderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * OpenAI 兼容模型提供者实现
 */
@Slf4j
@Primary
@Component
public class OpenAiModelProvider implements ModelProvider {

    public static final String CODE = "openai";

    private final AiProviderService providerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, ToolCallback> toolCallbacks = new ConcurrentHashMap<>();

    public OpenAiModelProvider(AiProviderService providerService) {
        this.providerService = providerService;
    }

    @Override
    public String getCode() {
        return CODE;
    }

    protected String getProviderCode() {
        return CODE;
    }

    @Override
    public AiModelResponse chat(List<Message> messages, AiChatOptions options) {
        AiModelProvider config = providerService.getEnabledProvider(getProviderCode());
        ChatClient chatClient = buildChatClient(config, options);
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt(new Prompt(messages));

        if (options.getSystemPrompt() != null && !options.getSystemPrompt().isEmpty()) {
            spec.system(options.getSystemPrompt());
        }

        if (Boolean.TRUE.equals(options.getEnableTools()) && !toolCallbacks.isEmpty()) {
            spec.tools(new ArrayList<>(toolCallbacks.values()));
        }

        ChatResponse response = spec.call().chatResponse();
        Generation generation = response.getResult();
        String content = generation != null && generation.getOutput() != null
                ? generation.getOutput().getText()
                : "";

        return AiModelResponse.builder()
                .content(content)
                .model(options.getModel() != null ? options.getModel() : config.getModel())
                .usage(buildUsage(response))
                .finishReason(generation != null && generation.getMetadata() != null
                        ? generation.getMetadata().getFinishReason()
                        : null)
                .build();
    }

    @Override
    public Flux<AiStreamChunk> streamChat(List<Message> messages, AiChatOptions options) {
        AiModelProvider config = providerService.getEnabledProvider(getProviderCode());
        ChatClient chatClient = buildChatClient(config, options);
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt(new Prompt(messages));

        if (options.getSystemPrompt() != null && !options.getSystemPrompt().isEmpty()) {
            spec.system(options.getSystemPrompt());
        }

        if (Boolean.TRUE.equals(options.getEnableTools()) && !toolCallbacks.isEmpty()) {
            spec.tools(new ArrayList<>(toolCallbacks.values()));
        }

        Sinks.Many<AiStreamChunk> sink = Sinks.many().unicast().onBackpressureBuffer();

        spec.stream().chatResponse()
                .doOnNext(response -> {
                    Generation generation = response.getResult();
                    if (generation != null && generation.getOutput() != null) {
                        String text = generation.getOutput().getText();
                        if (text != null) {
                            sink.tryEmitNext(AiStreamChunk.builder()
                                    .content(text)
                                    .done(false)
                                    .build());
                        }
                    }
                })
                .doOnComplete(() -> sink.tryEmitNext(AiStreamChunk.builder()
                        .content("")
                        .done(true)
                        .build()))
                .doOnError(error -> sink.tryEmitNext(AiStreamChunk.builder()
                        .errorMsg(error.getMessage())
                        .done(true)
                        .build()))
                .subscribe();

        return sink.asFlux();
    }

    @Override
    public void registerTool(String name, String description, String schema, Function<String, String> executor) {
        FunctionToolCallback.Builder<String, String> builder = FunctionToolCallback.builder(name, executor);
        builder.description(description);
        builder.inputSchema(schema);
        builder.inputType(String.class);
        toolCallbacks.put(name, builder.build());
    }

    private ChatClient buildChatClient(AiModelProvider config, AiChatOptions options) {
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(config.getEndpoint())
                .apiKey(config.getApiKey())
                .build();

        String model = options.getModel() != null && !options.getModel().isEmpty()
                ? options.getModel() : config.getModel();
        OpenAiChatOptions.Builder chatOptionsBuilder = OpenAiChatOptions.builder()
                .model(model);

        Map<String, Object> ext = parseConfig(config.getConfigJson());
        if (options.getTemperature() != null) {
            chatOptionsBuilder.temperature(options.getTemperature());
        } else if (ext.get("temperature") != null) {
            chatOptionsBuilder.temperature(parseDouble(ext.get("temperature")));
        }
        if (options.getMaxTokens() != null) {
            chatOptionsBuilder.maxTokens(options.getMaxTokens());
        } else if (ext.get("maxTokens") != null) {
            chatOptionsBuilder.maxTokens(parseInt(ext.get("maxTokens")));
        }

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(chatOptionsBuilder.build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    private Map<String, Object> parseConfig(String configJson) {
        try {
            if (configJson == null || configJson.isEmpty()) {
                return Map.of();
            }
            return objectMapper.readValue(configJson, Map.class);
        } catch (Exception e) {
            log.warn("解析模型配置失败: {}", configJson, e);
            return Map.of();
        }
    }

    private Double parseDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }

    private Integer parseInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private AiUsage buildUsage(ChatResponse response) {
        if (response.getMetadata() == null || response.getMetadata().getUsage() == null) {
            return null;
        }
        org.springframework.ai.chat.metadata.Usage usage = response.getMetadata().getUsage();
        return AiUsage.builder()
                .promptTokens(usage.getPromptTokens().longValue())
                .completionTokens(usage.getCompletionTokens().longValue())
                .totalTokens(usage.getTotalTokens().longValue())
                .build();
    }
}
