package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysResource;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysResourceMapper;
import com.xy.system.service.ISysResourceService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements ISysResourceService {

    private final SysResourceMapper sysResourceMapper;

    public SysResourceServiceImpl(SysResourceMapper sysResourceMapper) {
        this.sysResourceMapper = sysResourceMapper;
    }

    /**
     * 分页查询资源列表
     * */
    @Override
    public PageResult<SysResource> selectSysResourceList(SysResource sysResource) {

        if (sysResource.getPageNum() == 0) {
            sysResource.setPageNum(1);
        }

        if (sysResource.getPageSize() == 0) {
            sysResource.setPageSize(10);
        }

        LambdaQueryWrapper<SysResource> queryWrapper = new LambdaQueryWrapper<>();

        if (sysResource.getResourceName() != null && !StringUtils.isEmpty(sysResource.getResourceName())) {
            queryWrapper.like(SysResource::getResourceName, sysResource.getResourceName());
        }

        if (sysResource.getResourceKey() != null && !StringUtils.isEmpty(sysResource.getResourceKey())) {
            queryWrapper.like(SysResource::getResourceKey, sysResource.getResourceKey());
        }

        Page<SysResource> page = this.page(new Page<>(sysResource.getPageNum(), sysResource.getPageSize()), queryWrapper);

        return new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 新增资源
     * */
    public void addSysResource(SysResource sysResource) {

        if (sysResource.getResourceUrl() == null || StringUtils.isEmpty(sysResource.getResourceUrl())) {
            throw new SecurityException("新增资源失败");
        }

        save(sysResource);
    }

    /**
     * 更新资源
     * */
    public void updateResourceUrl(String url) {

        if (url == null || StringUtils.isEmpty(url)) {
            throw new SecurityException("url 不能为空");
        }

        LambdaUpdateWrapper<SysResource> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.set(SysResource::getResourceUrl, url);
        updateWrapper.eq(SysResource::getUpdateTime, now);

        sysResourceMapper.update(updateWrapper);
    }
}
