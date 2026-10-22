package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.system.mapper.todo.TodoTagMapper;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.ITodoTagService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTag;
import com.xy.common.utils.ContentCheckUtils;
import com.xy.common.utils.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TodoTagServiceImpl extends ServiceImpl<TodoTagMapper, TodoTag> implements ITodoTagService {

    /**
     * 分页查询标签列表
     * */
    @Override
    public PageResult<TodoTag> getTodoTags(TodoTag todoTag) {
        if (todoTag.getPageNum() == 0) todoTag.setPageNum(1);
        if (todoTag.getPageSize() == 0) todoTag.setPageSize(10);

        Long userId = TodoUserDetails.getCurrentUId();

        LambdaQueryWrapper<TodoTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(TodoTag::getUserId, userId);
        if (todoTag.getTagName() != null && !StringUtils.isEmpty(todoTag.getTagName())) {
            queryWrapper.like(TodoTag::getTagName, todoTag.getTagName());
        }

        Page<TodoTag> page = this.page(new Page<>(todoTag.getPageNum(), todoTag.getPageSize()), queryWrapper);

        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    /**
     * 查询默认标签列表
     * */
    public List<TodoTag> getDefaultTodoTags() {
        LambdaQueryWrapper<TodoTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(TodoTag::getUserId, 10000000L);

        return this.getBaseMapper().selectList(queryWrapper);
    }

    /**
     * 添加标签
     * */
    @Override
    public void addTodoTag(TodoTag todoTag) {
        ContentCheckUtils.check(todoTag.getTagName(), 2, 4);

        Long userId = TodoUserDetails.getCurrentUId();

        todoTag.setUserId(userId);
        todoTag.setCreateTime(LocalDateTime.now());
        todoTag.setUpdateTime(LocalDateTime.now());

        save(todoTag);
    }

    /**
     * 删除标签
     * */
    @Override
    public void removeTodoTag(Long tagId) {

        if (tagId == null) {
            throw new SecurityException("标签 Id不能为空");
        }

        removeById(tagId);
    }
}
