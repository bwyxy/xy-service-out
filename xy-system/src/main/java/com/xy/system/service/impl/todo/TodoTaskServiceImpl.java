package com.xy.system.service.impl.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.entity.TodoTask;
import com.xy.system.mapper.todo.TodoTaskMapper;
import com.xy.system.service.todo.ITodoTaskService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TodoTaskServiceImpl extends ServiceImpl<TodoTaskMapper, TodoTask> implements ITodoTaskService {

    /**
     * 根据开始时间结束时间查询待办任务列表
     * */
    @Override
    public List<TodoTask> listPendingTasks(LocalDateTime start, LocalDateTime end) {

        List<TodoTask> todoTasks;

        LambdaQueryWrapper<TodoTask> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.ge(TodoTask::getStartTime, start)
                .le(TodoTask::getDueDate, end);

        todoTasks = this.getBaseMapper().selectList(queryWrapper);

        return buildTaskList(todoTasks);
    }

    /**
     * 设置任务树形结构
     * */
    private List<TodoTask> buildTaskList(List<TodoTask> todoTasks) {
        Map<Long, TodoTask> map = todoTasks.stream()
                .collect(Collectors.toMap(TodoTask::getTaskId, t -> {
                   t.setTasks(new ArrayList<>());
                   return t;
                }));

        List<TodoTask> todoTaskList = new ArrayList<>();

        for (TodoTask todoTask : todoTasks) {
            if (todoTask.getParentId() == null) {
                todoTaskList.add(todoTask);
            } else {
                TodoTask parentTask = map.get(todoTask.getParentId());
                if (parentTask != null) {
                    parentTask.getTasks().add(todoTask);
                }
            }
        }

        return todoTaskList;
    }
}
