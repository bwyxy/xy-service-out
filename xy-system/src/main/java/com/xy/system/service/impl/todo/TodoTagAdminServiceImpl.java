package com.xy.system.service.impl.todo;

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
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoTagAdminServiceImpl implements ITodoTagAdminService {

    private final TodoTagMapper todoTagMapper;

    @Override
    public PageResult<TodoTag> selectTagList(TodoTag tag) {
        int pageNum = tag.getPageNum() <= 0 ? 1 : tag.getPageNum();
        int pageSize = tag.getPageSize() <= 0 ? 10 : tag.getPageSize();

        List<TodoTag> list = todoTagMapper.selectAdminTagList(tag);
        Long total = todoTagMapper.countAdminTagList(tag);

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        List<TodoTag> records = fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex);

        return PageResult.of(records, total, (long) pageNum, (long) pageSize);
    }

    @Override
    public TodoTag selectTagById(Long tagId) {
        if (tagId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签ID不能为空");
        }
        return todoTagMapper.selectAdminTagById(tagId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTag(TodoTag tag) {
        validateTag(tag);
        tag.setCreateTime(LocalDateTime.now());
        tag.setUpdateTime(LocalDateTime.now());
        todoTagMapper.insertAdminTag(tag);
        return tag.getTagId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTag(TodoTag tag) {
        if (tag.getTagId() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签ID不能为空");
        }
        validateTag(tag);
        TodoTag existing = todoTagMapper.selectAdminTagById(tag.getTagId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "标签不存在");
        }
        tag.setUpdateTime(LocalDateTime.now());
        return todoTagMapper.updateAdminTag(tag) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTag(Long tagId) {
        if (tagId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "标签ID不能为空");
        }
        TodoTag existing = todoTagMapper.selectAdminTagById(tagId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "标签不存在");
        }
        return todoTagMapper.deleteAdminTag(tagId) > 0;
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
