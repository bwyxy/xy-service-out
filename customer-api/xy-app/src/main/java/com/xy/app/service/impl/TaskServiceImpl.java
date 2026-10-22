package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xy.system.mapper.todo.TodoTaskMapper;
import com.xy.app.service.ITodoTaskCollaboratorService;
import com.xy.app.service.ITaskService;
import com.xy.app.service.ITodoTaskTagService;
import com.xy.app.security.TodoUserDetails;
import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.ToDoTaskDTO;
import com.xy.common.entity.TodoTask;
import com.xy.common.utils.ContentCheckUtils;
import com.xy.common.utils.StringUtils;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TaskServiceImpl extends ServiceImpl<TodoTaskMapper, TodoTask> implements ITaskService {

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

        Long userId = TodoUserDetails.getCurrentUId();

        todoTask.setUserId(userId);

        PageHelper.startPage(todoTask.getPageNum(), todoTask.getPageSize());

        List<TodoTask> todoTasks = todoTaskMapper.selectTaskByDate(todoTask);

        return getxyTaskPageResult((Page<TodoTask>) todoTasks);
    };

    /**
     * 查询任务列表
     * */
    @Override
    public PageResult<TodoTask> selectTaskList(TodoTask todoTask) {
        if (todoTask.getPageNum() == 0) todoTask.setPageNum(1);

        if (todoTask.getPageSize() == 0) todoTask.setPageSize(10);

        Long userId = TodoUserDetails.getCurrentUId();

        todoTask.setUserId(userId);

        PageHelper.startPage(todoTask.getPageNum(), todoTask.getPageSize());

        List<TodoTask> todoTasks = todoTaskMapper.selectTaskList(todoTask);

        return getxyTaskPageResult((Page<TodoTask>) todoTasks);
    }

    @NotNull
    private PageResult<TodoTask> getxyTaskPageResult(Page<TodoTask> todoTasks) {
        Page<TodoTask> page = todoTasks;

        PageResult<TodoTask> pageResult = new PageResult<>();

        pageResult.setList(page.getResult());
        pageResult.setTotal(page.getTotal());
        pageResult.setPageNum((long) page.getPageNum());
        pageResult.setPageSize((long) page.getPageSize());

        return pageResult;
    }

    ;

    /**
     * 根据任务 Id查询任务信息
     * */
    @Override
    public TodoTask selectTaskById(Long id) {
        if (id == null) {
            throw new SecurityException("任务 Id不能为空");
        }

        return todoTaskMapper.selectTaskById(id);
    }

    /**
     * 创建任务
     * */
    @Override
    public void createTask(ToDoTaskDTO TodoTaskDTO) {
        // 校验标题是否合规
        ContentCheckUtils.check(TodoTaskDTO.getTaskTitle(), 1 , 255);
        // 校验描述是否合规
        ContentCheckUtils.check(TodoTaskDTO.getDescription());

        Long userId = TodoUserDetails.getCurrentUId();
        LocalDateTime now = LocalDateTime.now();

        TodoTask todoTask = new TodoTask();

        if (TodoTaskDTO.getStartTime() != null) {
            todoTask.setStartTime(TodoTaskDTO.getStartTime());
        } else {
            todoTask.setStartTime(now);
        }

        if (TodoTaskDTO.getParentId() != null) {
            todoTask.setParentId(TodoTaskDTO.getParentId());
        }

        todoTask.setUserId(userId);
        todoTask.setTaskTitle(TodoTaskDTO.getTaskTitle());
        todoTask.setDescription(TodoTaskDTO.getDescription());
        todoTask.setCompleted(0L);
        todoTask.setUrgencyLevel(TodoTaskDTO.getUrgencyLevel());
        todoTask.setDueDate(TodoTaskDTO.getDueDate());

        boolean success = save(todoTask);

        // 将创建人加入为任务管理员
        if (success) {
            Long taskId = todoTask.getTaskId();
            todoTaskCollaboratorService.joinTaskCollaborator(userId, taskId, "1");
            // 添加标签关联
            if (TodoTaskDTO.getTagIds() != null && !TodoTaskDTO.getTagIds().isEmpty()) {
                todoTaskTagService.addTaskTags(taskId, TodoTaskDTO.getTagIds());
            }
        }
    }

    /**
     * 编辑任务
     * */
    @Override
    public void updateTask(ToDoTaskDTO TodoTaskDTO) {
        if (TodoTaskDTO.getTaskId() == null) {
            throw new SecurityException("任务 Id不能为空");
        }
        // 校验标题是否合规
        ContentCheckUtils.check(TodoTaskDTO.getTaskTitle(), 1 , 255);
        // 校验描述是否合规
        ContentCheckUtils.check(TodoTaskDTO.getDescription());

        LambdaUpdateWrapper<TodoTask> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper.eq(TodoTask::getTaskId, TodoTaskDTO.getTaskId());
        if (TodoTaskDTO.getTaskTitle() != null && !StringUtils.isEmpty(TodoTaskDTO.getTaskTitle())) {
            updateWrapper.set(TodoTask::getTaskTitle, TodoTaskDTO.getTaskTitle());
        }
        if (TodoTaskDTO.getDescription() != null) {
            updateWrapper.set(TodoTask::getDescription, TodoTaskDTO.getDescription());
        }
        if (TodoTaskDTO.getUrgencyLevel() != 0) {
            updateWrapper.set(TodoTask::getUrgencyLevel, TodoTaskDTO.getUrgencyLevel());
        }
        if (TodoTaskDTO.getStartTime() != null) {
            updateWrapper.set(TodoTask::getStartTime, TodoTaskDTO.getStartTime());
        }
        if (TodoTaskDTO.getDueDate() != null) {
            updateWrapper.set(TodoTask::getDueDate, TodoTaskDTO.getDueDate());
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
