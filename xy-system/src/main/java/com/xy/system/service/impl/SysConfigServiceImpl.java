package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysConfig;
import com.xy.common.enums.ConfigType;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysConfigMapper;
import com.xy.system.service.ISysConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

/**
 * 内容配置 逻辑层
 * */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements ISysConfigService {

    /**
     * 分页查询内容配置列表
     * */
    @Override
    public PageResult<SysConfig> findSysConfigs(SysConfig sysConfig) {
        if (sysConfig.getConfigType() == null || StringUtils.isEmpty(sysConfig.getConfigType())) {
            throw new SecurityException("配置类型不能为空");
        }

        if (sysConfig.getPageNum() == 0) {
            sysConfig.setPageNum(1);
        }

        if (sysConfig.getPageSize() == 0) {
            sysConfig.setPageSize(10);
        }

        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();

        if (sysConfig.getConfigName() != null && !StringUtils.isEmpty(sysConfig.getConfigName())) {
            queryWrapper.like(SysConfig::getConfigName, sysConfig.getConfigName());
        }

        if (sysConfig.getStatus() != null && !StringUtils.isEmpty(Collections.singleton(sysConfig.getStatus()))) {
            queryWrapper.eq(SysConfig::getStatus, sysConfig.getStatus());
        }

        queryWrapper.eq(SysConfig::getConfigType, sysConfig.getConfigType());
        queryWrapper.eq(SysConfig::getDeleted, 0);
        Page<SysConfig> page = this.page(new Page<>(sysConfig.getPageNum(), sysConfig.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据配置 ID查询配置信息
     * */
    @Override
    public SysConfig findSysConfigById(Long id) {
        if (id == null || StringUtils.isEmpty(id.toString())) {
            throw new SecurityException("配置 ID不能为空");
        }

        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysConfig::getConfigId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 添加配置
     * */
    @Override
    public void saveSysConfig(SysConfig sysConfig) {
        verificationContentItemParams(sysConfig);

        save(sysConfig);
    }

    /**
     * 更新配置
     * */
    @Override
    public void updateSysConfig(SysConfig sysConfig) {
        if (sysConfig.getConfigId() == null || StringUtils.isEmpty(sysConfig.getConfigId().toString())) {
            throw new SecurityException("配置 Id不能为空");
        }
        verificationContentItemParams(sysConfig);

        updateById(sysConfig);
    }

    /**
     * 删除配置
     * */
    @Override
    public void deleteSysConfigById(Long id) {
        if (id == null || StringUtils.isEmpty(id.toString())) {
            throw new SecurityException("配置 Id不能为空");
        }

        LambdaUpdateWrapper<SysConfig> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(SysConfig::getConfigId, id);

        LocalDateTime now = LocalDateTime.now();

        updateWrapper.set(SysConfig::getDeleted, 1);
        updateWrapper.set(SysConfig::getUpdateTime, now);

        update(updateWrapper);
    }

    /**
     * 校验内容配置参数
     *
     */
    private void verificationContentItemParams(SysConfig sysConfig) {
        if (sysConfig.getConfigType() == null || StringUtils.isEmpty(sysConfig.getConfigType())) {
            throw new SecurityException("配置类型不能为空");
        }

        if (sysConfig.getConfigName() == null || StringUtils.isEmpty(sysConfig.getConfigName())) {
            throw new SecurityException("配置名称不能为空");
        }

        ConfigType.valueOf(sysConfig.getConfigType());
    }
}
