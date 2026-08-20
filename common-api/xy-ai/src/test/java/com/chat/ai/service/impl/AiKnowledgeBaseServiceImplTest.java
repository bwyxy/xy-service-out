package com.chat.ai.service.impl;

import com.chat.ai.dto.KnowledgeBaseRequest;
import com.chat.ai.dto.KnowledgeDocumentRequest;
import com.chat.ai.service.AiKnowledgeBaseService;
import com.chat.ai.vo.KnowledgeBaseVo;
import com.xy.common.core.domain.PageResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Sql(scripts = "/schema-test.sql")
@Transactional
class AiKnowledgeBaseServiceImplTest {

    @Autowired
    private AiKnowledgeBaseService knowledgeBaseService;

    @Test
    void shouldCreateKnowledgeBase() {
        KnowledgeBaseRequest request = new KnowledgeBaseRequest();
        request.setName("测试知识库");
        request.setDescription("描述");
        request.setEmbeddingModel("text-embedding-v3");
        request.setProviderCode("openai");

        assertTrue(knowledgeBaseService.saveOrUpdateKb(request));

        PageResult<KnowledgeBaseVo> page = knowledgeBaseService.pageKbs("测试", 1, 10);
        assertEquals(1, page.getList().size());
    }

    @Test
    void shouldAddDocumentAndRetrieve() {
        KnowledgeBaseRequest kbRequest = new KnowledgeBaseRequest();
        kbRequest.setName("产品手册");
        knowledgeBaseService.saveOrUpdateKb(kbRequest);

        KnowledgeBaseVo kb = knowledgeBaseService.pageKbs(null, 1, 1).getList().get(0);

        KnowledgeDocumentRequest docRequest = new KnowledgeDocumentRequest();
        docRequest.setKbId(kb.getId());
        docRequest.setTitle("产品介绍");
        docRequest.setContent("我们的产品是一款 AI 助手，支持对话、RAG 和工具调用。");
        docRequest.setSourceType("TEXT");

        assertTrue(knowledgeBaseService.addDocument(docRequest));

        // 由于 EmbeddingModel 在测试环境返回空向量，检索结果为空；此处验证接口不抛异常
        List<String> chunks = knowledgeBaseService.retrieveChunks(kb.getId(), "AI 助手", 3);
        assertNotNull(chunks);
    }
}
