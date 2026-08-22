package com.chat.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chat.ai.dto.ModelProviderRequest;
import com.chat.ai.exception.AiException;
import com.xy.common.entity.ai.AiModelProvider;
import com.xy.common.enums.ai.ProviderType;
import com.xy.system.mapper.ai.AiModelProviderMapper;
import com.chat.ai.service.AiProviderService;
import com.chat.ai.vo.ModelProviderVo;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * AI 模型提供者服务实现
 */
@Service
public class AiProviderServiceImpl extends ServiceImpl<AiModelProviderMapper, AiModelProvider>
        implements AiProviderService {

    @Override
    public AiModelProvider getDefaultProvider() {
        LambdaQueryWrapper<AiModelProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelProvider::getEnabled, 1)
                .orderByDesc(AiModelProvider::getPriority)
                .last("limit 1");
        AiModelProvider provider = getOne(wrapper);
        if (provider == null) {
            throw new AiException("未配置可用的 AI 模型提供者");
        }
        return provider;
    }

    @Override
    public AiModelProvider getEnabledProvider(String code) {
        if (code == null || code.isEmpty()) {
            return getDefaultProvider();
        }
        LambdaQueryWrapper<AiModelProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiModelProvider::getCode, code)
                .eq(AiModelProvider::getEnabled, 1)
                .last("limit 1");
        AiModelProvider provider = getOne(wrapper);
        if (provider == null) {
            throw new AiException("模型提供者未启用或不存在: " + code);
        }
        return provider;
    }

    @Override
    public List<ModelProviderVo> listProviders() {
        List<AiModelProvider> list = list();
        return list.stream()
                .sorted(Comparator.comparing(AiModelProvider::getPriority).reversed())
                .map(this::convertToVo)
                .toList();
    }

    @Override
    public boolean saveOrUpdateProvider(ModelProviderRequest request) {
        AiModelProvider entity = new AiModelProvider();
        BeanUtils.copyProperties(request, entity);
        if (request.getType() != null) {
            ProviderType.of(request.getType());
        }
        if (entity.getPriority() == null) {
            entity.setPriority(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        entity.setUpdateTime(LocalDateTime.now());
        if (entity.getId() == null) {
            entity.setCreateTime(LocalDateTime.now());
        }
        return saveOrUpdate(entity);
    }

    @Override
    public boolean toggleProvider(Long id) {
        AiModelProvider provider = getById(id);
        if (provider == null) {
            return false;
        }
        provider.setEnabled(provider.getEnabled() == 1 ? 0 : 1);
        provider.setUpdateTime(LocalDateTime.now());
        return updateById(provider);
    }

    private ModelProviderVo convertToVo(AiModelProvider provider) {
        ModelProviderVo vo = new ModelProviderVo();
        BeanUtils.copyProperties(provider, vo);
        return vo;
    }
}
