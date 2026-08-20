package com.chat.ai.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 嵌入向量服务
 */
@Slf4j
@Service
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;

    public EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * 对文本进行向量化
     */
    public List<Double> embed(String text) {
        try {
            EmbeddingResponse response = embeddingModel.call(
                    new EmbeddingRequest(List.of(text), null));
            if (response.getResults() == null || response.getResults().isEmpty()) {
                return Collections.emptyList();
            }
            float[] output = response.getResults().get(0).getOutput();
            List<Double> vector = new java.util.ArrayList<>(output.length);
            for (float v : output) {
                vector.add((double) v);
            }
            return vector;
        } catch (Exception e) {
            log.warn("嵌入向量生成失败，返回空向量: {}", text.substring(0, Math.min(text.length(), 50)), e);
            return Collections.emptyList();
        }
    }
}
