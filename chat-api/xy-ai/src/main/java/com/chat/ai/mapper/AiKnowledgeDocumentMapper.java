package com.chat.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.ai.entity.AiKnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 知识库文档 Mapper
 */
@Mapper
public interface AiKnowledgeDocumentMapper extends BaseMapper<AiKnowledgeDocument> {
}
