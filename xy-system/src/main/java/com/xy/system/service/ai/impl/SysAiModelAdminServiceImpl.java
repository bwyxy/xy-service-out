package com.xy.system.service.ai.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.AiModelProviderPageQuery;
import com.xy.common.dto.ai.AiModelProviderRequest;
import com.xy.common.entity.ai.AiModelProvider;
import com.xy.common.enums.ai.ProviderType;
import com.xy.common.exception.ServiceException;
import com.xy.common.vo.ai.AiModelProviderVo;
import com.xy.system.mapper.ai.AiModelProviderMapper;
import com.xy.system.service.ai.ISysAiModelAdminService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 模型提供者后台管理服务实现
 */
@Service
public class SysAiModelAdminServiceImpl implements ISysAiModelAdminService {

    private final AiModelProviderMapper providerMapper;

    public SysAiModelAdminServiceImpl(AiModelProviderMapper providerMapper) {
        this.providerMapper = providerMapper;
    }

    @Override
    public PageResult<AiModelProviderVo> pageProviders(AiModelProviderPageQuery query) {
        LambdaQueryWrapper<AiModelProvider> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(query.getKeyword()), AiModelProvider::getName, query.getKeyword())
                .or()
                .like(StringUtils.hasText(query.getKeyword()), AiModelProvider::getCode, query.getKeyword())
                .eq(query.getEnabled() != null, AiModelProvider::getEnabled, query.getEnabled())
                .orderByDesc(AiModelProvider::getPriority)
                .orderByDesc(AiModelProvider::getCreateTime);

        Page<AiModelProvider> page = providerMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        List<AiModelProviderVo> records = page.getRecords().stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
        return new PageResult<>(records, page.getTotal(), (long) query.getPageNum(), (long) query.getPageSize());
    }

    @Override
    public AiModelProviderVo getProviderById(Long id) {
        AiModelProvider provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new ServiceException("模型提供者不存在");
        }
        return convertToVo(provider);
    }

    @Override
    public R<Void> saveOrUpdateProvider(AiModelProviderRequest request) {
        if (request.getType() != null) {
            ProviderType.of(request.getType());
        }

        AiModelProvider entity = new AiModelProvider();
        BeanUtils.copyProperties(request, entity);
        if (entity.getPriority() == null) {
            entity.setPriority(0);
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        entity.setUpdateTime(LocalDateTime.now());

        if (entity.getId() == null) {
            LambdaQueryWrapper<AiModelProvider> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AiModelProvider::getCode, entity.getCode());
            if (providerMapper.selectCount(wrapper) > 0) {
                throw new ServiceException("提供者编码已存在");
            }
            entity.setCreateTime(LocalDateTime.now());
            providerMapper.insert(entity);
        } else {
            providerMapper.updateById(entity);
        }
        return R.ok();
    }

    @Override
    public R<Void> deleteProvider(Long id) {
        if (providerMapper.deleteById(id) <= 0) {
            throw new ServiceException("模型提供者不存在或删除失败");
        }
        return R.ok();
    }

    @Override
    public R<Void> toggleProvider(Long id) {
        AiModelProvider provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new ServiceException("模型提供者不存在");
        }
        provider.setEnabled(provider.getEnabled() == null || provider.getEnabled() == 0 ? 1 : 0);
        provider.setUpdateTime(LocalDateTime.now());
        providerMapper.updateById(provider);
        return R.ok();
    }

    private AiModelProviderVo convertToVo(AiModelProvider provider) {
        AiModelProviderVo vo = new AiModelProviderVo();
        BeanUtils.copyProperties(provider, vo);
        return vo;
    }
}
