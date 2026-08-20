package com.chat.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chat.ai.entity.AiUserQuota;

/**
 * AI 用户配额服务
 */
public interface AiQuotaService extends IService<AiUserQuota> {

    /**
     * 检查并消耗一次调用配额
     */
    boolean consumeQuota(Long userId, String userType);

    /**
     * 获取或初始化用户配额
     */
    AiUserQuota getOrInitQuota(Long userId, String userType);
}
