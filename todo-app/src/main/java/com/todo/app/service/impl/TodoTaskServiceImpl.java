package com.todo.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.todo.app.mapper.TodoTaskMapper;
import com.todo.app.service.ITodoTaskCollaboratorService;
import com.todo.app.service.ITodoTaskService;
import com.todo.app.service.ITodoTaskTagService;
import com.todo.common.core.domain.PageResult;
import com.todo.common.dto.ToDoTaskDTO;
import com.todo.common.entity.TodoTask;
import com.todo.common.utils.ContentCheckUtils;
import com.todo.common.utils.SecurityUtils;
import com.todo.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TodoTaskServiceImpl extends ServiceImpl<TodoTaskMapper, TodoTask> implements ITodoTaskService {

    @Autowired
    private TodoTaskMapper todoTaskMapper;

    @Autowired
    private ITodoTaskCollaboratorService todoTaskCollaboratorService;

    @Autowired
    private ITodoTaskTagService todoTaskTagService;

    /**
     * 根据传入时间 查询当天任务
     * */
    @Override
    public PageResult<TodoTask> selectTaskByDate(TodoTask todoTask) {
        if (todoTask.getPageNum() == 0) todoTask.setPageNum(1);

        if (todoTask.getPageSize() == 0) todoTask.setPageSize(10);

        if (todoTask.getStartTime() == null) todoTask.setStartTime(LocalDateTime.now());

        LocalDateTime end = todoTask.getStartTime().toLocalDate().atTime(23, 59, 59);
        if (todoTask.getDueDate() != null) {
            end = todoTask.getDueDate().toLocalDate().atTime(23, 59, 59);
        }
        todoTask.setDueDate(end);

        Long userId = SecurityUtils.getUserId();

        todoTask.setUserId(userId);

        PageHelper.startPage(todoTask.getPageNum(), todoTask.getPageSize());

        List<TodoTask> todoTasks = todoTaskMapper.selectTaskByDate(todoTask);

        Page<TodoTask> page = (Page<TodoTask>) todoTasks;

        PageResult<TodoTask> pageResult = new PageResult<>();

        pageResult.setList(page.getResult());
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum((long) page.getPageNum());
        pageResult.setPageSize((long) page.getPageSize());

        return pageResult;
    };

    /**
     * 根据任务 Id查询任务信息
     * */
    @Override
    public TodoTask selectTaskById(Long id) {
        if (id == null) {
            throw new SecurityException("任务 Id不能为空");
        }

        LambdaQueryWrapper<TodoTask> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(TodoTask::getTaskId, id);

        return this.getOne(queryWrapper);
    }

    /**
     * 创建任务
     * */
    @Override
    public void createTask(ToDoTaskDTO toDoTaskDTO) {
        // 校验标题是否合规
        ContentCheckUtils.check(toDoTaskDTO.getTaskTitle(), 1 , 255);
        // 校验描述是否合规
        ContentCheckUtils.check(toDoTaskDTO.getDescription());

        Long userId = SecurityUtils.getUserId();
        LocalDateTime now = LocalDateTime.now();

        TodoTask todoTask = new TodoTask();

        if (toDoTaskDTO.getStartTime() != null) {
            todoTask.setStartTime(toDoTaskDTO.getStartTime());
        } else {
            todoTask.setStartTime(now);
        }

        todoTask.setUserId(userId);
        todoTask.setTaskTitle(toDoTaskDTO.getTaskTitle());
        todoTask.setDescription(toDoTaskDTO.getDescription());
        todoTask.setCompleted(0L);
        todoTask.setUrgencyLevel(toDoTaskDTO.getUrgencyLevel());
        todoTask.setDueDate(toDoTaskDTO.getDueDate());

        boolean success = save(todoTask);

        // 将创建人加入为任务管理员
        if (success) {
            Long taskId = todoTask.getTaskId();
            todoTaskCollaboratorService.joinTaskCollaborator(userId, taskId, "1");
            // 添加标签关联
            if (toDoTaskDTO.getTagIds() != null && !toDoTaskDTO.getTagIds().isEmpty()) {
                todoTaskTagService.addTaskTags(taskId, toDoTaskDTO.getTagIds());
            }
        }
    }

    /**
     * 编辑任务
     * */
    @Override
    public void updateTask(ToDoTaskDTO toDoTaskDTO) {
        if (toDoTaskDTO.getTaskId() == null) {
            throw new SecurityException("任务 Id不能为空");
        }
        // 校验标题是否合规
        ContentCheckUtils.check(toDoTaskDTO.getTaskTitle(), 1 , 255);
        // 校验描述是否合规
        ContentCheckUtils.check(toDoTaskDTO.getDescription());

        LambdaUpdateWrapper<TodoTask> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(TodoTask::getTaskId, toDoTaskDTO.getTaskId());
        if (toDoTaskDTO.getTaskTitle() != null && !StringUtils.isEmpty(toDoTaskDTO.getTaskTitle())) {
            updateWrapper.set(TodoTask::getTaskTitle, toDoTaskDTO.getTaskTitle());
        }
        if (toDoTaskDTO.getDescription() != null) {
            updateWrapper.set(TodoTask::getDescription, toDoTaskDTO.getDescription());
        }
        if (toDoTaskDTO.getUrgencyLevel() != 0) {
            updateWrapper.set(TodoTask::getUrgencyLevel, toDoTaskDTO.getUrgencyLevel());
        }
        if (toDoTaskDTO.getStartTime() != null) {
            updateWrapper.set(TodoTask::getStartTime, toDoTaskDTO.getStartTime());
        }
        if (toDoTaskDTO.getDueDate() != null) {
            updateWrapper.set(TodoTask::getDueDate, toDoTaskDTO.getDueDate());
        }
        updateWrapper.set(TodoTask::getUpdateTime, LocalDateTime.now());

        update(updateWrapper);
    }

    /**
     * 设置任务完成状态
     * */
    @Override
    public void setTaskCompleted(TodoTask todoTask) {
        if (todoTask.getTaskId() == null) {
            throw new SecurityException("任务 Id不能为空");
        }

        LambdaUpdateWrapper<TodoTask> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(TodoTask::getTaskId, todoTask.getTaskId());
        updateWrapper.set(TodoTask::getCompleted, todoTask.getCompleted());
        if (todoTask.getCompleted() == 1) {
            updateWrapper.set(TodoTask::getCompletionTime, LocalDateTime.now());
        }
        updateWrapper.set(TodoTask::getUpdateTime, LocalDateTime.now());

        update(updateWrapper);
    }

    /**
     * 删除任务
     * */
    @Override
    public void deleteTaskByTaskId(Long taskId) {
        if (taskId == null) {
            throw new SecurityException("任务 Id不能为空");
        }

        LambdaUpdateWrapper<TodoTask> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(TodoTask::getTaskId, taskId);
        updateWrapper.set(TodoTask::getDeleted, 1);
        updateWrapper.set(TodoTask::getUpdateTime, LocalDateTime.now());

        update(updateWrapper);
    }
}
