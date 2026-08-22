package com.chat.ai.rag;

import com.xy.common.entity.ai.AiKnowledgeChunk;
import com.xy.system.mapper.ai.AiKnowledgeChunkMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 本地向量存储服务
 * <p>
 * 基于 MySQL 存储向量并在内存中计算余弦相似度。
 * 生产环境建议替换为 Redis Vector / Milvus / Pinecone 等专用向量数据库。
 */
@Slf4j
@Service
public class VectorStoreService {

    private final AiKnowledgeChunkMapper chunkMapper;

    private final EmbeddingModelFactory embeddingModelFactory;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public VectorStoreService(AiKnowledgeChunkMapper chunkMapper,
                              EmbeddingModelFactory embeddingModelFactory) {
        this.chunkMapper = chunkMapper;
        this.embeddingModelFactory = embeddingModelFactory;
    }

    /**
     * 添加文档片段到向量库
     *
     * @param kbId           知识库 ID
     * @param docId          文档 ID
     * @param content        片段内容
     * @param metadata       元数据
     * @param providerCode   嵌入模型提供者编码
     * @param embeddingModel 嵌入模型名称
     */
    public void addChunk(Long kbId, Long docId, String content, Map<String, Object> metadata,
                         String providerCode, String embeddingModel) {
        List<Double> embedding = embed(content, providerCode, embeddingModel);
        AiKnowledgeChunk chunk = new AiKnowledgeChunk();
        chunk.setKbId(kbId);
        chunk.setDocId(docId);
        chunk.setContent(content);
        try {
            chunk.setEmbeddingJson(objectMapper.writeValueAsString(embedding));
            chunk.setMetadataJson(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.warn("序列化向量或元数据失败", e);
        }
        chunk.setCreateTime(java.time.LocalDateTime.now());
        chunkMapper.insert(chunk);
    }

    /**
     * 检索最相似的 topK 个片段
     *
     * @param kbId           知识库 ID
     * @param query          查询文本
     * @param topK           返回数量
     * @param providerCode   嵌入模型提供者编码
     * @param embeddingModel 嵌入模型名称
     */
    public List<String> search(Long kbId, String query, int topK,
                               String providerCode, String embeddingModel) {
        List<Double> queryEmbedding = embed(query, providerCode, embeddingModel);
        if (queryEmbedding.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiKnowledgeChunk> chunks = chunkMapper.selectByKbId(kbId);
        return chunks.stream()
                .map(chunk -> new AbstractMap.SimpleEntry<>(chunk, cosineSimilarity(queryEmbedding, parseEmbedding(chunk.getEmbeddingJson()))))
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.<AiKnowledgeChunk, Double>comparingByValue().reversed())
                .limit(topK)
                .map(entry -> entry.getKey().getContent())
                .collect(Collectors.toList());
    }

    /**
     * 删除文档的所有片段
     */
    public void deleteByDocId(Long docId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiKnowledgeChunk> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(AiKnowledgeChunk::getDocId, docId);
        chunkMapper.delete(wrapper);
    }

    private List<Double> embed(String text, String providerCode, String embeddingModel) {
        log.info("生成 Embedding：providerCode={}, embeddingModel={}", providerCode, embeddingModel);
        try {
            EmbeddingModel model = embeddingModelFactory.getModel(providerCode, embeddingModel);
            if (model == null) {
                log.warn("未获取到 EmbeddingModel，返回空向量");
                return Collections.emptyList();
            }
            EmbeddingResponse response = model.call(new EmbeddingRequest(List.of(text), null));
            if (response.getResults() == null || response.getResults().isEmpty()) {
                return Collections.emptyList();
            }
            float[] output = response.getResults().get(0).getOutput();
            List<Double> vector = new ArrayList<>(output.length);
            for (float v : output) {
                vector.add((double) v);
            }
            return vector;
        } catch (Exception e) {
            log.warn("嵌入向量生成失败，返回空向量: {}", text.substring(0, Math.min(text.length(), 50)), e);
            return Collections.emptyList();
        }
    }

    private List<Double> parseEmbedding(String embeddingJson) {
        try {
            if (embeddingJson == null || embeddingJson.isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.readValue(embeddingJson, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            log.warn("解析向量失败: {}", embeddingJson, e);
            return Collections.emptyList();
        }
    }

    private double cosineSimilarity(List<Double> a, List<Double> b) {
        if (a.size() != b.size() || a.isEmpty()) {
            return 0.0;
        }
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.size(); i++) {
            double av = a.get(i);
            double bv = b.get(i);
            dot += av * bv;
            normA += av * av;
            normB += bv * bv;
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
