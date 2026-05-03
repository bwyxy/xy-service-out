package com.todo.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.common.core.domain.PageResult;
import com.todo.common.entity.SysTag;
import com.todo.common.exception.ServiceException;
import com.todo.common.utils.StringUtils;
import com.todo.system.mapper.SysTagMapper;
import com.todo.system.service.ISysTagService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysTagServiceImpl extends ServiceImpl<SysTagMapper, SysTag> implements ISysTagService {

    private final SysTagMapper sysTagMapper;

    public SysTagServiceImpl(SysTagMapper sysTagMapper) {
        this.sysTagMapper = sysTagMapper;
    }

    /**
     * 分页查询标签列表
     * */
    @Override
    public PageResult<SysTag> selectSysTagList(SysTag sysTag) {

        if (sysTag.getPageNum() == 0) {
            sysTag.setPageNum(1);
        }

        if (sysTag.getPageSize() == 0) {
            sysTag.setPageSize(10);
        }

        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();

        if (sysTag.getName() != null && !StringUtils.isEmpty(sysTag.getName())) {
            queryWrapper.like(SysTag::getName, sysTag.getName());
        }

        Page<SysTag> page = this.page(new Page<>(sysTag.getPageNum(), sysTag.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据标签 Id查询标签信息
     * */
    @Override
    public SysTag selectSysTagById(Long id) {
        if (id == null) {
            throw new SecurityException("标签 ID不能为空");
        }

        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysTag::getTagId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 新增标签
     * */
    @Override
    public void insertSysTag(SysTag sysTag) {

        if (sysTag.getName() == null || StringUtils.isEmpty(sysTag.getName())) {
            throw new ServiceException("标签名称不能为空");
        }

        save(sysTag);
    }

    /**
     * 更新标签信息
     * */
    @Override
    public void updateSysTag(SysTag sysTag) {
        if (sysTag.getTagId() == null) {
            throw new SecurityException("标签 Id不能为空");
        }

        LambdaUpdateWrapper<SysTag> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(SysTag::getTagId, sysTag.getTagId());

        if (sysTag.getName() != null && !StringUtils.isEmpty(sysTag.getName())) {
            updateWrapper.set(SysTag::getName, sysTag.getName());
        }

        if (sysTag.getColor() != null && !StringUtils.isEmpty(sysTag.getColor())) {
            updateWrapper.set(SysTag::getColor, sysTag.getColor());
        }

        if (sysTag.getSlug() != null && !StringUtils.isEmpty(sysTag.getSlug())) {
            updateWrapper.set(SysTag::getSlug, sysTag.getSlug());
        }

        if (sysTag.getDescription() != null && !StringUtils.isEmpty(sysTag.getDescription())) {
            updateWrapper.set(SysTag::getDescription, sysTag.getDescription());
        }

        updateWrapper.set(SysTag::getUpdateTime, now);

        sysTagMapper.update(updateWrapper);
    }

    /**
     * 删除标签
     * */
    @Override
    public void deleteSysTagById(Long id) {
        if (id == null) {
            throw new SecurityException("标签 Id不能为空");
        }

        LambdaQueryWrapper<SysTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysTag::getTagId, id);

        remove(queryWrapper);
    }
}
