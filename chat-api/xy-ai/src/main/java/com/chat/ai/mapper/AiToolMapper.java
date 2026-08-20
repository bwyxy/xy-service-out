package com.chat.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.ai.entity.AiTool;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 工具注册 Mapper
 */
@Mapper
public interface AiToolMapper extends BaseMapper<AiTool> {
}
