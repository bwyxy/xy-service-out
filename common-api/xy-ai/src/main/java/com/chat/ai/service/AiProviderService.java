package com.chat.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.ai.dto.ModelProviderRequest;
import com.xy.common.entity.ai.AiModelProvider;
import com.chat.ai.vo.ModelProviderVo;

import java.util.List;

/**
 * AI 模型提供者服务
 */
public interface AiProviderService extends IService<AiModelProvider> {

    /**
     * 获取默认启用的模型提供者
     */
    AiModelProvider getDefaultProvider();

    /**
     * 根据编码获取启用的模型提供者
     */
    AiModelProvider getEnabledProvider(String code);

    /**
     * 列出所有提供者
     */
    List<ModelProviderVo> listProviders();

    /**
     * 新增或更新提供者
     */
    boolean saveOrUpdateProvider(ModelProviderRequest request);

    /**
     * 切换提供者启用状态
     */
    boolean toggleProvider(Long id);
}
