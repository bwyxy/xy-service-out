package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysCategory;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysCategoryMapper;
import com.xy.system.service.ISysCategoryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SysCategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory> implements ISysCategoryService {

    private final SysCategoryMapper sysCategoryMapper;

    public SysCategoryServiceImpl(SysCategoryMapper sysCategoryMapper) {
        this.sysCategoryMapper = sysCategoryMapper;
    }

    /**
     * 分页查询分类列表
     * */
    @Override
    public PageResult<SysCategory> selectSysCategoryList(SysCategory sysCategory) {

        if (sysCategory.getPageNum() == 0) {
            sysCategory.setPageNum(1);
        }

        if (sysCategory.getPageSize() == 0) {
            sysCategory.setPageSize(10);
        }

        Page<SysCategory> page = new Page<>(sysCategory.getPageNum(), sysCategory.getPageSize());
        List<SysCategory> records = sysCategoryMapper.selectSysCategoryList(page, sysCategory);

        return PageResult.of(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 根据分类 Id查询分类信息
     * */
    @Override
    public SysCategory selectSysCategoryById(Long id) {
        if (id == null) {
            throw new SecurityException("分类 Id不能为空");
        }

        LambdaQueryWrapper<SysCategory> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(SysCategory::getCategoryId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 新增分类
     * */
    @Override
    public void insertSysCategory(SysCategory sysCategory) {

        if (sysCategory.getName() == null || StringUtils.isEmpty(sysCategory.getName())) {
            throw new SecurityException("分类名称不能为空");
        }

        save(sysCategory);
    }

    /**
     * 更新分类
     * */
    @Override
    public void updateSysCategory(SysCategory sysCategory) {
        if (sysCategory.getCategoryId() == null) {
            throw new SecurityException("分类 Id不能为空");
        }

        LambdaUpdateWrapper<SysCategory> updateWrapper = new LambdaUpdateWrapper<>();
        LocalDateTime now = LocalDateTime.now();

        updateWrapper.eq(SysCategory::getCategoryId, sysCategory.getCategoryId());

        if (sysCategory.getName() != null && !StringUtils.isEmpty(sysCategory.getName())) {
            updateWrapper.set(SysCategory::getName, sysCategory.getName());
        }

        if (sysCategory.getSlug() != null && !StringUtils.isEmpty(sysCategory.getSlug())) {
            updateWrapper.set(SysCategory::getSlug, sysCategory.getSlug());
        }

        if (sysCategory.getDescription() != null && !StringUtils.isEmpty(sysCategory.getDescription())) {
            updateWrapper.set(SysCategory::getDescription, sysCategory.getDescription());
        }

        updateWrapper.set(SysCategory::getUpdateTime, now);

        sysCategoryMapper.update(updateWrapper);
    }

    /**
     * 删除分类
     * */
    @Override
    public void deleteSysCategoryById(Long id) {
        if (id == null) {
            throw new SecurityException("分类 Id不能为空");
        }

        LambdaUpdateWrapper<SysCategory> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(SysCategory::getCategoryId, id);

        remove(updateWrapper);
    }
}
