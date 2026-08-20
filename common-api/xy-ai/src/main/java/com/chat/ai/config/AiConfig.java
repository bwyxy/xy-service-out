package com.chat.ai.config;

import com.chat.ai.entity.AiModelProvider;
import com.chat.ai.service.AiProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 模块配置
 */
@Slf4j
@Configuration
public class AiConfig {

    /**
     * 向量存储：默认使用内存版 SimpleVectorStore。
     * 生产环境可通过条件装配替换为 Redis/Milvus 实现。
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 嵌入模型：默认使用 OpenAI 兼容接口的 embedding 模型。
     * 若未配置 API Key，调用时会返回空向量（由 EmbeddingService 捕获）。
     */
    @Bean
    public EmbeddingModel embeddingModel(AiProviderService providerService) {
        AiModelProvider provider;
        try {
            provider = providerService.getEnabledProvider("openai");
        } catch (Exception e) {
            log.warn("未找到 OpenAI 提供者，使用空 EmbeddingModel");
            return new EmptyEmbeddingModel();
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(provider.getEndpoint())
                .apiKey(provider.getApiKey())
                .build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model("text-embedding-v3")
                .build();
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }
}
