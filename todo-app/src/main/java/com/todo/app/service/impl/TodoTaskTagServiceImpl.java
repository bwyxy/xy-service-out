package com.todo.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.todo.app.mapper.TodoTaskTagMapper;
import com.todo.app.service.ITodoTaskTagService;
import com.todo.common.entity.TodoTaskTag;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TodoTaskTagServiceImpl extends ServiceImpl<TodoTaskTagMapper, TodoTaskTag> implements ITodoTaskTagService {

    /**
     * 添加任务和标签的关联
     * */
    @Override
    public void addTaskTags(Long taskId, List<Long> tagIds) {
        batchAddTagsForTask(taskId, tagIds);
    }

    /**
     * 更新添加任务和标签的关联
     * */
    @Transactional
    public void updateTaskTags(Long taskId, List<Long> tagIds) {
        LambdaQueryWrapper<TodoTaskTag> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(TodoTaskTag::getTaskId, taskId);

        remove(queryWrapper);

        batchAddTagsForTask(taskId, tagIds);
    }

    /**
     * 批量添加标签和文章的关联
     * */
    private void batchAddTagsForTask(Long taskId, List<Long> tagIds) {
        if (taskId == null) {
            throw new SecurityException("任务 Id不能为空");
        }

        List<TodoTaskTag> list = tagIds.stream()
                .map(tagId -> {
                    TodoTaskTag todoTaskTag = new TodoTaskTag();
                    todoTaskTag.setTaskId(taskId);
                    todoTaskTag.setTagId(tagId);
                    return todoTaskTag;
                })
                .collect(Collectors.toList());

        saveBatch(list);
    }
}
