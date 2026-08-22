package com.xy.system.mapper.ai;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.ai.AiKnowledgeChunk;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI 知识库文档片段 Mapper
 */
@Mapper
public interface AiKnowledgeChunkMapper extends BaseMapper<AiKnowledgeChunk> {

    @Select("SELECT * FROM ai_knowledge_chunk WHERE kb_id = #{kbId}")
    List<AiKnowledgeChunk> selectByKbId(@Param("kbId") Long kbId);
}
