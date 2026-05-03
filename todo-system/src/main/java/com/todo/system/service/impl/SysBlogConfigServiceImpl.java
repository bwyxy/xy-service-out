package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.entity.SysBlogConfig;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysBlogConfigMapper;
import com.todo.system.service.ISysBlogConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysBlogConfigServiceImpl extends ServiceImpl<SysBlogConfigMapper, SysBlogConfig> implements ISysBlogConfigService {

    private final SysBlogConfigMapper sysBlogConfigMapper;

    public SysBlogConfigServiceImpl(SysBlogConfigMapper sysBlogConfigMapper) {
        this.sysBlogConfigMapper = sysBlogConfigMapper;
    }

    /**
     * 查询博客配置列表
     * */
    @Override
    public List<SysBlogConfig> selectBlogConfigList(SysBlogConfig sysBlogConfig) {

        LambdaQueryWrapper<SysBlogConfig> queryWrapper = new LambdaQueryWrapper<>();

        if (sysBlogConfig.getConfigKey() != null && !StringUtils.isEmpty(sysBlogConfig.getConfigKey())) {
            queryWrapper.like(SysBlogConfig::getConfigKey, sysBlogConfig.getConfigKey());
        }

        if (sysBlogConfig.getId() != null) {
            queryWrapper.eq(SysBlogConfig::getId, sysBlogConfig.getId());
        }

        return this.getBaseMapper().selectList(queryWrapper);
    }

    /**
     * 根据配置 ID 查询博客配置信息
     * */
    @Override
    public SysBlogConfig selectBlogConfigById(Long id) {
        LambdaQueryWrapper<SysBlogConfig> queryWrapper = new LambdaQueryWrapper<>();

        if (id == null) {
            throw new SecurityException("配置 Id不能为空");
        }
        queryWrapper.eq(SysBlogConfig::getId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 新增配置信息
     * */
    @Override
    public void saveBlogConfig(SysBlogConfig sysBlogConfig) {

        if (sysBlogConfig.getConfigKey() != null && StringUtils.isEmpty(sysBlogConfig.getConfigKey())) {
            throw new SecurityException("配置标识不能为空");
        }

        save(sysBlogConfig);
    }

    /**
     * 更新配置信息
     * */
    @Override
    public void updateBlogConfig(SysBlogConfig sysBlogConfig) {

        if (sysBlogConfig.getId() == null) {
            throw new SecurityException("配置 Id不能为空");
        }

        LambdaUpdateWrapper<SysBlogConfig> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(SysBlogConfig::getId, sysBlogConfig.getId());

        if (sysBlogConfig.getConfigKey() != null && !StringUtils.isEmpty(sysBlogConfig.getConfigKey())) {
            updateWrapper.set(SysBlogConfig::getConfigKey, sysBlogConfig.getConfigKey());
        }

        if (sysBlogConfig.getConfigValue() != null && !StringUtils.isEmpty(sysBlogConfig.getConfigValue())) {
            updateWrapper.set(SysBlogConfig::getConfigValue, sysBlogConfig.getConfigValue());
        }

        if (sysBlogConfig.getDescription() != null && !StringUtils.isEmpty(sysBlogConfig.getDescription())) {
            updateWrapper.set(SysBlogConfig::getDescription, sysBlogConfig.getDescription());
        }

        updateWrapper.set(SysBlogConfig::getUpdateTime, now);

        sysBlogConfigMapper.update(updateWrapper);
    }
}
