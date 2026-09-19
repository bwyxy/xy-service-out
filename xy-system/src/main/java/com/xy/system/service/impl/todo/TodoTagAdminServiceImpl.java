package com.xy.system.service.impl.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTag;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.todo.TodoTagMapper;
import com.xy.system.service.todo.ITodoTagAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TodoTagAdminServiceImpl implements ITodoTagAdminService {

    private final TodoTagMapper todoTagMapper;

    @Override
    public PageResult<TodoTag> selectTagList(TodoTag tag) {
        int pageNum = tag.getPageNum() <= 0 ? 1 : tag.getPageNum();
        int pageSize = tag.getPageSize() <= 0 ? 10 : tag.getPageSize();

        LambdaQueryWrapper<TodoTag> queryWrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(tag.getTagName())) {
            queryWrapper.like(TodoTag::getTagName, tag.getTagName());
        }
        if (tag.getUserId() != null) {
            queryWrapper.eq(TodoTag::getUserId, tag.getUserId());
        }
        queryWrapper.orderByDesc(TodoTag::getCreateTime);

        Page<TodoTag> page = todoTagMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public TodoTag selectTagById(Long tagId) {
        if (tagId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签ID不能为空");
        }
        return todoTagMapper.selectById(tagId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTag(TodoTag tag) {
        validateTag(tag);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        todoTagMapper.insert(tag);
        return tag.getTagId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTag(TodoTag tag) {
        if (tag.getTagId() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签ID不能为空");
        }
        validateTag(tag);
        TodoTag existing = todoTagMapper.selectById(tag.getTagId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "标签不存在");
        }
        tag.setUpdateTime(LocalDateTime.now());
        return todoTagMapper.updateById(tag) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Long tagId) {
        if (tagId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签ID不能为空");
        }
        TodoTag existing = todoTagMapper.selectById(tagId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "标签不存在");
        }
        return todoTagMapper.deleteById(tagId) > 0;
    }

    private void validateTag(TodoTag tag) {
        if (StringUtils.isEmpty(tag.getTagName())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签名称不能为空");
        }
        if (tag.getTagName().length() > 24) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签名称长度不能超过24个字符");
        }
    }
}
