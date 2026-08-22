package com.xy.system.service.ai;

import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.AiModelProviderPageQuery;
import com.xy.common.dto.ai.AiModelProviderRequest;
import com.xy.common.vo.ai.AiModelProviderVo;

/**
 * AI 模型提供者后台管理服务
 */
public interface ISysAiModelAdminService {

    /**
     * 分页查询模型提供者
     */
    PageResult<AiModelProviderVo> pageProviders(AiModelProviderPageQuery query);

    /**
     * 根据 ID 查询模型提供者
     */
    AiModelProviderVo getProviderById(Long id);

    /**
     * 新增或更新模型提供者
     */
    R<Void> saveOrUpdateProvider(AiModelProviderRequest request);

    /**
     * 删除模型提供者
     */
    R<Void> deleteProvider(Long id);

    /**
     * 切换启用状态
     */
    R<Void> toggleProvider(Long id);
}
