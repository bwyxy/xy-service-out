package com.chat.ai.rag;

import com.chat.ai.dto.KnowledgeBaseRequest;
import com.chat.ai.dto.KnowledgeDocumentRequest;
import com.chat.ai.service.AiKnowledgeBaseService;
import com.chat.ai.service.AiProviderService;
import com.chat.ai.vo.KnowledgeBaseVo;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 双轨 Embedding 集成测试：验证不同知识库可以使用不同的 provider/model
 */
@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-test.sql")
@Transactional
@Import(DualTrackEmbeddingIntegrationTest.TestConfig.class)
class DualTrackEmbeddingIntegrationTest {

    @Autowired
    private RecordingEmbeddingModelFactory recordingFactory;

    @Autowired
    private AiKnowledgeBaseService knowledgeBaseService;

    /**
     * 固定返回 3 维向量的 mock EmbeddingModel，保证测试不依赖真实网络调用
     */
    private static EmbeddingModel fixedEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest request) {
                List<Embedding> embeddings = new ArrayList<>();
                int index = 0;
                for (String text : request.getInstructions()) {
                    float[] vector = new float[3];
                    // 用文本 hash 填充向量，相同文本向量相同，便于余弦相似度计算
                    float base = (text.hashCode() % 1000) / 1000f;
                    vector[0] = base;
                    vector[1] = base * 0.5f;
                    vector[2] = base * 0.25f;
                    embeddings.add(new Embedding(vector, index++));
                }
                return new EmbeddingResponse(embeddings);
            }

            @Override
            public float[] embed(String text) {
                return new float[0];
            }

            @Override
            public float[] embed(org.springframework.ai.document.Document document) {
                return new float[0];
            }
        };
    }

    @Test
    void shouldRouteToDifferentProviderPerKnowledgeBase() {
        // 知识库 A：使用 openai 提供者
        KnowledgeBaseRequest kbA = new KnowledgeBaseRequest();
        kbA.setName("KB-OpenAI");
        kbA.setProviderCode("openai");
        kbA.setEmbeddingModel("text-embedding-v3");
        knowledgeBaseService.saveOrUpdateKb(kbA);

        // 知识库 B：使用 dashscope 提供者
        KnowledgeBaseRequest kbB = new KnowledgeBaseRequest();
        kbB.setName("KB-DashScope");
        kbB.setProviderCode("dashscope-embedding");
        kbB.setEmbeddingModel("text-embedding-v2");
        knowledgeBaseService.saveOrUpdateKb(kbB);

        List<KnowledgeBaseVo> kbs = knowledgeBaseService.pageKbs(null, 1, 10).getList();
        KnowledgeBaseVo voA = kbs.stream().filter(k -> k.getName().equals("KB-OpenAI")).findFirst().orElseThrow();
        KnowledgeBaseVo voB = kbs.stream().filter(k -> k.getName().equals("KB-DashScope")).findFirst().orElseThrow();

        // 向知识库 A 添加文档
        KnowledgeDocumentRequest docA = new KnowledgeDocumentRequest();
        docA.setKbId(voA.getId());
        docA.setTitle("OpenAI 文档");
        docA.setContent("这是 OpenAI embedding 模型处理的文档内容。");
        docA.setSourceType("TEXT");
        assertTrue(knowledgeBaseService.addDocument(docA));

        // 向知识库 B 添加文档
        KnowledgeDocumentRequest docB = new KnowledgeDocumentRequest();
        docB.setKbId(voB.getId());
        docB.setTitle("DashScope 文档");
        docB.setContent("这是 DashScope embedding 模型处理的文档内容。");
        docB.setSourceType("TEXT");
        assertTrue(knowledgeBaseService.addDocument(docB));

        // 验证：不同知识库的文档入库时， factory 被传入了各自的 provider_code / embedding_model
        assertTrue(recordingFactory.hasCall("openai", "text-embedding-v3"),
                "知识库 A 应使用 openai / text-embedding-v3");
        assertTrue(recordingFactory.hasCall("dashscope-embedding", "text-embedding-v2"),
                "知识库 B 应使用 dashscope-embedding / text-embedding-v2");

        // 验证：检索时也会按知识库各自的 provider/model 路由
        List<String> chunksA = knowledgeBaseService.retrieveChunks(voA.getId(), "OpenAI", 3);
        List<String> chunksB = knowledgeBaseService.retrieveChunks(voB.getId(), "DashScope", 3);
        assertNotNull(chunksA);
        assertNotNull(chunksB);

        assertEquals(2, recordingFactory.countCalls("openai", "text-embedding-v3"),
                "入库 + 检索各一次，共两次");
        assertEquals(2, recordingFactory.countCalls("dashscope-embedding", "text-embedding-v2"),
                "入库 + 检索各一次，共两次");
    }

    @Test
    void shouldFallbackToDefaultWhenProviderCodeIsEmpty() {
        // 知识库 C：未指定 provider_code，应使用全局默认 EmbeddingModel
        KnowledgeBaseRequest kbC = new KnowledgeBaseRequest();
        kbC.setName("KB-Default");
        knowledgeBaseService.saveOrUpdateKb(kbC);

        KnowledgeBaseVo voC = knowledgeBaseService.pageKbs(null, 1, 1).getList().get(0);

        KnowledgeDocumentRequest docC = new KnowledgeDocumentRequest();
        docC.setKbId(voC.getId());
        docC.setTitle("默认文档");
        docC.setContent("这是使用全局默认 embedding 模型的文档内容。");
        docC.setSourceType("TEXT");
        assertTrue(knowledgeBaseService.addDocument(docC));

        // providerCode 和 embeddingModel 均为空，factory 应收到空字符串
        assertTrue(recordingFactory.hasCall("", ""),
                "未配置 provider 时应回退到全局默认 EmbeddingModel");
    }

    /**
     * 测试配置：用 RecordingEmbeddingModelFactory 替换真实 Factory，记录每次调用参数
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public RecordingEmbeddingModelFactory recordingEmbeddingModelFactory(
                @Qualifier("embeddingModel") EmbeddingModel defaultEmbeddingModel,
                AiProviderService providerService) {
            return new RecordingEmbeddingModelFactory(defaultEmbeddingModel, providerService);
        }
    }

    /**
     * 记录调用参数的 EmbeddingModelFactory 测试替身
     */
    static class RecordingEmbeddingModelFactory extends EmbeddingModelFactory {

        private final List<String[]> calls = new ArrayList<>();

        public RecordingEmbeddingModelFactory(EmbeddingModel defaultEmbeddingModel,
                                              AiProviderService providerService) {
            super(defaultEmbeddingModel, providerService);
        }

        @Override
        public EmbeddingModel getModel(String providerCode, String embeddingModel) {
            calls.add(new String[]{providerCode == null ? "" : providerCode,
                    embeddingModel == null ? "" : embeddingModel});
            return fixedEmbeddingModel();
        }

        public boolean hasCall(String providerCode, String embeddingModel) {
            return countCalls(providerCode, embeddingModel) > 0;
        }

        public int countCalls(String providerCode, String embeddingModel) {
            int count = 0;
            for (String[] call : calls) {
                if (call[0].equals(providerCode) && call[1].equals(embeddingModel)) {
                    count++;
                }
            }
            return count;
        }
    }
}
