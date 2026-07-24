package com.xy.app.service;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTag;

import java.util.List;

public interface ITodoTagService {

    /**
     * 分页查询标签列表
     * */
    public PageResult<TodoTag> getTodoTags(TodoTag todoTag);

    /**
     * 查询默认标签列表
     * */
    public List<TodoTag> getDefaultTodoTags();

    /**
     * 添加标签
     * */
    public void addTodoTag(TodoTag todoTag);

    /**
     * 删除标签
     * */
    public void removeTodoTag(Long tagId);
}
