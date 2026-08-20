package com.chat.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.ai.entity.AiModelProvider;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模型提供者 Mapper
 */
@Mapper
public interface AiModelProviderMapper extends BaseMapper<AiModelProvider> {
}
