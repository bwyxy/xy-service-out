package com.chat.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.ai.dto.KnowledgeBaseRequest;
import com.chat.ai.dto.KnowledgeDocumentRequest;
import com.chat.ai.entity.AiKnowledgeBase;
import com.chat.ai.vo.KnowledgeBaseVo;
import com.xy.common.core.domain.PageResult;

import java.util.List;

/**
 * AI 知识库服务
 */
public interface AiKnowledgeBaseService extends IService<AiKnowledgeBase> {

    /**
     * 创建/更新知识库
     */
    boolean saveOrUpdateKb(KnowledgeBaseRequest request);

    /**
     * 分页查询知识库
     */
    PageResult<KnowledgeBaseVo> pageKbs(String keyword, int pageNum, int pageSize);

    /**
     * 向知识库添加文档
     */
    boolean addDocument(KnowledgeDocumentRequest request);

    /**
     * 删除知识库文档
     */
    boolean removeDocument(Long docId);

    /**
     * 检索知识库片段
     */
    List<String> retrieveChunks(Long kbId, String query, int topK);
}
