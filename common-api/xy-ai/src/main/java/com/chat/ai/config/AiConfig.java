package com.chat.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.StringUtils;

/**
 * AI 模块配置
 */
@Slf4j
@Configuration
public class AiConfig {

    @Value("${ai.embedding.base-url}")
    private String embeddingBaseUrl;

    @Value("${ai.embedding.api-key}")
    private String embeddingApiKey;

    @Value("${ai.embedding.model}")
    private String embeddingModelName;

    /**
     * 向量存储：默认使用内存版 SimpleVectorStore。
     * 生产环境可通过条件装配替换为 Redis/Milvus 实现。
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 全局默认嵌入模型：从 ai.embedding.* 配置读取，支持独立配置 Embedding Key/Endpoint/Model。
     * 未配置 API Key 时退化为空 EmbeddingModel，避免启动或调用时直接报错。
     * 知识库也可在 ai_knowledge_base 表中按 provider_code + embedding_model 单独指定嵌入模型。
     */
    @Primary
    @Bean
    public EmbeddingModel embeddingModel() {
        if (!StringUtils.hasText(embeddingApiKey)) {
            log.warn("未配置 ai.embedding.api-key，使用空 EmbeddingModel，RAG 知识库检索将返回空结果");
            return new EmptyEmbeddingModel();
        }
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(embeddingBaseUrl)
                .apiKey(embeddingApiKey)
                .build();
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();
        log.info("默认 EmbeddingModel 已初始化：baseUrl={}, model={}, apiKey={}",
                embeddingBaseUrl, embeddingModelName, maskKey(embeddingApiKey));
        return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
    }

    private String maskKey(String key) {
        if (!StringUtils.hasText(key) || key.length() <= 8) {
            return "***";
        }
        return key.substring(0, 4) + "****" + key.substring(key.length() - 4);
    }
}
