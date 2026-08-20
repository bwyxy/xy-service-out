package com.chat.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chat.ai.entity.AiUserQuota;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 用户配额 Mapper
 */
@Mapper
public interface AiUserQuotaMapper extends BaseMapper<AiUserQuota> {
}
