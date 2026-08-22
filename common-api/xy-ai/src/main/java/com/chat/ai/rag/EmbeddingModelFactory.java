package com.chat.ai.rag;

import com.xy.common.entity.ai.AiModelProvider;
import com.chat.ai.service.AiProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Embedding 模型工厂
 * <p>
 * 支持根据 ai_model_provider 表中的提供者动态创建 EmbeddingModel，
 * 未指定提供者时使用全局默认 EmbeddingModel。
 */
@Slf4j
@Component
public class EmbeddingModelFactory {

    private final EmbeddingModel defaultEmbeddingModel;

    private final AiProviderService providerService;

    public EmbeddingModelFactory(EmbeddingModel defaultEmbeddingModel,
                                 AiProviderService providerService) {
        this.defaultEmbeddingModel = defaultEmbeddingModel;
        this.providerService = providerService;
    }

    /**
     * 获取 EmbeddingModel。
     *
     * @param providerCode   提供者编码，为空时使用默认 EmbeddingModel
     * @param embeddingModel 向量模型名称，为空时使用提供者配置的模型
     * @return EmbeddingModel
     */
    public EmbeddingModel getModel(String providerCode, String embeddingModel) {
        if (!StringUtils.hasText(providerCode)) {
            return defaultEmbeddingModel;
        }
        AiModelProvider provider;
        try {
            provider = providerService.getEnabledProvider(providerCode);
        } catch (Exception e) {
            log.warn("未找到启用的 Embedding 提供者：{}，使用默认 EmbeddingModel", providerCode);
            return defaultEmbeddingModel;
        }
        if (!StringUtils.hasText(provider.getApiKey())) {
            log.warn("Embedding 提供者 {} 未配置 API Key，使用默认 EmbeddingModel", providerCode);
            return defaultEmbeddingModel;
        }
        String model = StringUtils.hasText(embeddingModel) ? embeddingModel : provider.getModel();
        if (!StringUtils.hasText(model)) {
            model = "text-embedding-v3";
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(provider.getEndpoint())
                .apiKey(provider.getApiKey())
                .build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(model)
                .build();
        log.info("动态创建 EmbeddingModel：provider={}, endpoint={}, model={}, apiKey={}",
                providerCode, provider.getEndpoint(), model, maskKey(provider.getApiKey()));
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    private String maskKey(String key) {
        if (!StringUtils.hasText(key) || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
