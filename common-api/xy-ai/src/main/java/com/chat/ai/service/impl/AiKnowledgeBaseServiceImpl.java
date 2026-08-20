package com.chat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.ai.dto.KnowledgeBaseRequest;
import com.chat.ai.dto.KnowledgeDocumentRequest;
import com.chat.ai.entity.AiKnowledgeBase;
import com.chat.ai.entity.AiKnowledgeDocument;
import com.chat.ai.enums.DocumentStatus;
import com.chat.ai.enums.KnowledgeBaseStatus;
import com.chat.ai.mapper.AiKnowledgeBaseMapper;
import com.chat.ai.mapper.AiKnowledgeDocumentMapper;
import com.chat.ai.rag.DocumentChunker;
import com.chat.ai.rag.VectorStoreService;
import com.chat.ai.service.AiKnowledgeBaseService;
import com.chat.ai.vo.KnowledgeBaseVo;
import com.xy.common.core.domain.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 知识库服务实现
 */
@Slf4j
@Service
public class AiKnowledgeBaseServiceImpl extends ServiceImpl<AiKnowledgeBaseMapper, AiKnowledgeBase>
        implements AiKnowledgeBaseService {

    private final AiKnowledgeDocumentMapper documentMapper;

    private final DocumentChunker documentChunker;

    private final VectorStoreService vectorStoreService;

    public AiKnowledgeBaseServiceImpl(AiKnowledgeDocumentMapper documentMapper,
                                      DocumentChunker documentChunker,
                                      VectorStoreService vectorStoreService) {
        this.documentMapper = documentMapper;
        this.documentChunker = documentChunker;
        this.vectorStoreService = vectorStoreService;
    }

    @Override
    public boolean saveOrUpdateKb(KnowledgeBaseRequest request) {
        AiKnowledgeBase entity = new AiKnowledgeBase();
        BeanUtils.copyProperties(request, entity);
        if (entity.getStatus() == null) {
            entity.setStatus(KnowledgeBaseStatus.ENABLED.getCode());
        }
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getId() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        return saveOrUpdate(entity);
    }

    @Override
    public PageResult<KnowledgeBaseVo> pageKbs(String keyword, int pageNum, int pageSize) {
        LambdaQueryWrapper<AiKnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(keyword), AiKnowledgeBase::getName, keyword)
                .orderByDesc(AiKnowledgeBase::getCreateTime);
        Page<AiKnowledgeBase> page = page(new Page<>(pageNum, pageSize), wrapper);
        List<KnowledgeBaseVo> records = page.getRecords().stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), (long) pageNum, (long) pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addDocument(KnowledgeDocumentRequest request) {
        AiKnowledgeBase kb = getById(request.getKbId());
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在");
        }

        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setKbId(request.getKbId());
        document.setTitle(request.getTitle());
        document.setSourceType(request.getSourceType());
        document.setSourceUrl(request.getSourceUrl());
        document.setContent(request.getContent());
        document.setStatus(DocumentStatus.PROCESSING.getCode());
        document.setCreateTime(LocalDateTime.now());
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.insert(document);

        try {
            List<String> chunks = documentChunker.chunk(request.getContent(), null, null);
            int count = 0;
            for (int i = 0; i < chunks.size(); i++) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("docId", document.getId());
                metadata.put("index", i);
                vectorStoreService.addChunk(request.getKbId(), document.getId(), chunks.get(i), metadata);
                count++;
            }
            document.setChunkCount(count);
            document.setStatus(DocumentStatus.COMPLETED.getCode());
        } catch (Exception e) {
            log.warn("知识库文档分片或向量化失败", e);
            document.setStatus(DocumentStatus.FAILED.getCode());
            document.setErrorMsg(e.getMessage());
        }
        document.setUpdateTime(LocalDateTime.now());
        documentMapper.updateById(document);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocument(Long docId) {
        vectorStoreService.deleteByDocId(docId);
        return documentMapper.deleteById(docId) > 0;
    }

    @Override
    public List<String> retrieveChunks(Long kbId, String query, int topK) {
        AiKnowledgeBase kb = getById(kbId);
        if (kb == null || KnowledgeBaseStatus.DISABLED.getCode() == kb.getStatus()) {
            return Collections.emptyList();
        }
        return vectorStoreService.search(kbId, query, topK);
    }

    private KnowledgeBaseVo convertToVo(AiKnowledgeBase kb) {
        KnowledgeBaseVo vo = new KnowledgeBaseVo();
        BeanUtils.copyProperties(kb, vo);
        return vo;
    }
}
