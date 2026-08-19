package com.xy.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.SysDictType;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.SysDictTypeMapper;
import com.xy.system.service.ISysDictTypeService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

/**
 * 字典类型服务实现
 * */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements ISysDictTypeService {

    @Override
    public PageResult<SysDictType> selectDictTypeList(SysDictType dictType) {
        int pageNum = dictType.getPageNum() <= 0 ? 1 : dictType.getPageNum();
        int pageSize = dictType.getPageSize() <= 0 ? 10 : dictType.getPageSize();

        LambdaQueryWrapper<SysDictType> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotEmpty(dictType.getDictName())) {
            queryWrapper.like(SysDictType::getDictName, dictType.getDictName());
        }
        if (StringUtils.isNotEmpty(dictType.getDictType())) {
            queryWrapper.like(SysDictType::getDictType, dictType.getDictType());
        }
        if (StringUtils.isNotEmpty(dictType.getStatus())) {
            queryWrapper.eq(SysDictType::getStatus, dictType.getStatus());
        }
        queryWrapper.orderByDesc(SysDictType::getCreateTime);

        Page<SysDictType> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public SysDictType selectDictTypeById(Long dictId) {
        return getById(dictId);
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public Long insertDictType(SysDictType dictType) {
        if (!checkDictTypeUnique(dictType)) {
            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "字典类型已存在");
        }
        if (StringUtils.isEmpty(dictType.getStatus())) {
            dictType.setStatus("0");
        }
        boolean success = save(dictType);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "新增字典类型失败");
        }
        return dictType.getDictId();
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public boolean updateDictType(SysDictType dictType) {
        SysDictType existing = getById(dictType.getDictId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "字典类型不存在");
        }
        if (StringUtils.isNotEmpty(dictType.getDictType())
                && !dictType.getDictType().equals(existing.getDictType())) {
            if (!checkDictTypeUnique(dictType)) {
                throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "字典类型已存在");
            }
        }
        boolean success = updateById(dictType);
        if (!success) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "修改字典类型失败");
        }
        return true;
    }

    @Override
    @CacheEvict(value = "dict", allEntries = true)
    public boolean deleteDictTypeById(Long dictId) {
        SysDictType existing = getById(dictId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "字典类型不存在");
        }
        return getBaseMapper().deleteById(dictId) > 0;
    }

    @Override
    public boolean checkDictTypeUnique(SysDictType dictType) {
        Long dictId = dictType.getDictId() == null ? -1L : dictType.getDictId();
        LambdaQueryWrapper<SysDictType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysDictType::getDictType, dictType.getDictType());
        SysDictType existing = getOne(queryWrapper);
        return existing == null || existing.getDictId().equals(dictId);
    }
}
