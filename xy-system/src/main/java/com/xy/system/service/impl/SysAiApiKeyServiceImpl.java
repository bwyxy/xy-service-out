package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.dto.ai.ApiKeyGenerateResponse;
import com.xy.common.dto.ai.ApiKeyPageQuery;
import com.xy.common.dto.ai.ApiKeyVo;
import com.xy.common.dto.ai.TokenResponse;
import com.xy.common.entity.AiApiKey;
import com.xy.common.core.domain.PageResult;
import com.xy.common.exception.ServiceException;
import com.xy.common.provider.JwtTokenProvider;
import com.xy.common.utils.ApiKeyGenerator;
import com.xy.system.mapper.SysAiApiKeyMapper;
import com.xy.system.service.ISysAiApiKeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI API 密钥服务实现
 */
@Service
public class SysAiApiKeyServiceImpl extends ServiceImpl<SysAiApiKeyMapper, AiApiKey>
        implements ISysAiApiKeyService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    public TokenResponse exchangeToken(String apiKey, String apiSecret) {
        if (apiKey == null || apiKey.isEmpty() || apiSecret == null || apiSecret.isEmpty()) {
            throw new ServiceException("apiKey 和 apiSecret 不能为空");
        }

        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiApiKey::getApiKey, apiKey).last("limit 1");
        AiApiKey key = getOne(wrapper);

        if (key == null) {
            throw new ServiceException("API Key 不存在");
        }
        if (key.getEnabled() == null || key.getEnabled() != 1) {
            throw new ServiceException("API Key 已被禁用");
        }
        if (key.getExpireTime() != null && key.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new ServiceException("API Key 已过期");
        }
        if (!passwordEncoder.matches(apiSecret, key.getApiSecret())) {
            if (!apiSecret.equals(key.getApiSecret())) {
                throw new ServiceException("API Secret 不正确");
            }
        }

        long expireSeconds = key.getTokenExpireSeconds() != null ? key.getTokenExpireSeconds() : 3600L;
        long expireMillis = expireSeconds * 1000L;
        String accessToken = jwtTokenProvider.createToken(key.getApiKey(), expireMillis);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expireSeconds)
                .build();
    }

    @Override
    public ApiKeyGenerateResponse generate(String appName, Integer tokenExpireSeconds) {
        if (tokenExpireSeconds == null || tokenExpireSeconds <= 0) {
            tokenExpireSeconds = 3600;
        }

        String plainSecret = ApiKeyGenerator.generateSecret();
        String apiKey = ApiKeyGenerator.generateApiKey();

        AiApiKey entity = new AiApiKey();
        entity.setAppName(appName);
        entity.setApiKey(apiKey);
        entity.setApiSecret(passwordEncoder.encode(plainSecret));
        entity.setEnabled(1);
        entity.setTokenExpireSeconds(tokenExpireSeconds);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        save(entity);

        return ApiKeyGenerateResponse.builder()
                .apiKey(apiKey)
                .apiSecret(plainSecret)
                .appName(appName)
                .tokenExpireSeconds(tokenExpireSeconds)
                .expireTime(entity.getExpireTime())
                .build();
    }

    @Override
    public PageResult<ApiKeyVo> pageKeys(ApiKeyPageQuery query) {
        LambdaQueryWrapper<AiApiKey> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(query.getAppName() != null && !query.getAppName().isEmpty(),
                AiApiKey::getAppName, query.getAppName());
        wrapper.eq(query.getApiKey() != null && !query.getApiKey().isEmpty(),
                AiApiKey::getApiKey, query.getApiKey());
        wrapper.eq(query.getEnabled() != null, AiApiKey::getEnabled, query.getEnabled());
        wrapper.orderByDesc(AiApiKey::getCreateTime);

        int pageNum = query.getPageNum() != null && query.getPageNum() > 0 ? query.getPageNum() : 1;
        int pageSize = query.getPageSize() != null && query.getPageSize() > 0 ? query.getPageSize() : 10;
        Page<AiApiKey> page = page(new Page<>(pageNum, pageSize), wrapper);

        List<ApiKeyVo> records = page.getRecords().stream().map(this::convertToVo).toList();
        return new PageResult<>(records, page.getTotal(), (long) pageNum, (long) pageSize);
    }

    private ApiKeyVo convertToVo(AiApiKey entity) {
        ApiKeyVo vo = new ApiKeyVo();
        vo.setId(entity.getId());
        vo.setAppName(entity.getAppName());
        vo.setApiKey(entity.getApiKey());
        vo.setEnabled(entity.getEnabled());
        vo.setExpireTime(entity.getExpireTime());
        vo.setTokenExpireSeconds(entity.getTokenExpireSeconds());
        vo.setCreateTime(entity.getCreateTime());
        vo.setUpdateTime(entity.getUpdateTime());
        return vo;
    }

    @Override
    public void enable(Long id) {
        AiApiKey entity = getById(id);
        if (entity == null) {
            throw new ServiceException("API Key 不存在");
        }
        entity.setEnabled(1);
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void disable(Long id) {
        AiApiKey entity = getById(id);
        if (entity == null) {
            throw new ServiceException("API Key 不存在");
        }
        entity.setEnabled(0);
        entity.setUpdateTime(LocalDateTime.now());
        updateById(entity);
    }

    @Override
    public void delete(Long id) {
        if (getBaseMapper().deleteById(id) <= 0) {
            throw new ServiceException("API Key 不存在或删除失败");
        }
    }
}
