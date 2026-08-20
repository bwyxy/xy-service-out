package com.chat.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.ai.entity.AiApiKey;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 模块 API 密钥 Mapper
 */
@Mapper
public interface AiApiKeyMapper extends BaseMapper<AiApiKey> {
}
