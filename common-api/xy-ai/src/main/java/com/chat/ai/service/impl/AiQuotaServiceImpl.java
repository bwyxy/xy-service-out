package com.chat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.ai.entity.AiUserQuota;
import com.chat.ai.enums.UserType;
import com.chat.ai.exception.AiException;
import com.chat.ai.mapper.AiUserQuotaMapper;
import com.chat.ai.service.AiQuotaService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AI 用户配额服务实现
 */
@Service
public class AiQuotaServiceImpl extends ServiceImpl<AiUserQuotaMapper, AiUserQuota>
        implements AiQuotaService {

    private static final int DEFAULT_DAILY_LIMIT = 100;

    @Override
    public boolean consumeQuota(Long userId, String userType) {
        AiUserQuota quota = getOrInitQuota(userId, userType);
        LocalDateTime now = LocalDateTime.now();
        if (quota.getResetTime() == null || quota.getResetTime().getDayOfYear() != now.getDayOfYear()
                || quota.getResetTime().getYear() != now.getYear()) {
            quota.setDailyUsed(0);
            quota.setResetTime(now);
        }
        if (quota.getDailyUsed() >= quota.getDailyLimit()) {
            throw new AiException("今日 AI 调用额度已用完，请明日再试");
        }
        quota.setDailyUsed(quota.getDailyUsed() + 1);
        quota.setTotalUsed(quota.getTotalUsed() + 1);
        quota.setUpdateTime(now);
        return updateById(quota);
    }

    @Override
    public AiUserQuota getOrInitQuota(Long userId, String userType) {
        UserType type = UserType.of(userType);
        LambdaQueryWrapper<AiUserQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiUserQuota::getUserId, userId)
                .eq(AiUserQuota::getUserType, type.getCode())
                .last("limit 1");
        AiUserQuota quota = getOne(wrapper);
        if (quota == null) {
            quota = new AiUserQuota();
            quota.setUserId(userId);
            quota.setUserType(type.getCode());
            quota.setDailyLimit(DEFAULT_DAILY_LIMIT);
            quota.setDailyUsed(0);
            quota.setTotalUsed(0L);
            quota.setResetTime(LocalDateTime.now());
            quota.setCreateTime(LocalDateTime.now());
            quota.setUpdateTime(LocalDateTime.now());
            save(quota);
        }
        return quota;
    }
}
