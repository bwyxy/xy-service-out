package com.chat.ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;

import java.util.Collections;
import java.util.List;

/**
 * 空嵌入模型实现，用于未配置 Embedding 服务时避免启动失败。
 */
public class EmptyEmbeddingModel implements EmbeddingModel {

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = request.getInstructions().stream()
                .map(text -> new Embedding(new float[0], 0))
                .toList();
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(Document document) {
        return new float[0];
    }
}
