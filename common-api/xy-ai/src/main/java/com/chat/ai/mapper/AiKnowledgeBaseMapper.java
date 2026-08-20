package com.chat.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.ai.entity.AiKnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 知识库 Mapper
 */
@Mapper
public interface AiKnowledgeBaseMapper extends BaseMapper<AiKnowledgeBase> {
}
