package com.xy.system.service.impl.todo;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTask;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.mapper.todo.TodoTaskMapper;
import com.xy.system.service.todo.ITodoTaskAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoTaskAdminServiceImpl implements ITodoTaskAdminService {

    private final TodoTaskMapper todoTaskMapper;

    @Override
    public PageResult<TodoTask> selectTaskList(TodoTask task) {
        int pageNum = task.getPageNum() <= 0 ? 1 : task.getPageNum();
        int pageSize = task.getPageSize() <= 0 ? 10 : task.getPageSize();

        LambdaQueryWrapper<TodoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TodoTask::getDeleted, 0);
        if (!StringUtils.isEmpty(task.getTaskTitle())) {
            queryWrapper.like(TodoTask::getTaskTitle, task.getTaskTitle());
        }
        if (task.getUserId() != null) {
            queryWrapper.eq(TodoTask::getUserId, task.getUserId());
        }
        if (task.getCompleted() != null) {
            queryWrapper.eq(TodoTask::getCompleted, task.getCompleted());
        }
        if (task.getUrgencyLevel() != 0) {
            queryWrapper.eq(TodoTask::getUrgencyLevel, task.getUrgencyLevel());
        }
        queryWrapper.orderByDesc(TodoTask::getCreateTime);

        Page<TodoTask> page = todoTaskMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper);
        return PageResult.of(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public TodoTask selectTaskById(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务ID不能为空");
        }
        return todoTaskMapper.selectById(taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addTask(TodoTask task) {
        validateTask(task);
        if (task.getCompleted() == null) {
            task.setCompleted(0L);
        }
        if (task.getUrgencyLevel() == 0) {
            task.setUrgencyLevel(1);
        }
        if (task.getUserId() == null) {
            task.setUserId(SecurityUtils.getUserId());
        }
        task.setDeleted(0);
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        todoTaskMapper.insert(task);
        return task.getTaskId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(TodoTask task) {
        if (task.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务ID不能为空");
        }
        validateTask(task);
        TodoTask existing = todoTaskMapper.selectById(task.getTaskId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        task.setUpdateTime(LocalDateTime.now());
        return todoTaskMapper.updateById(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务ID不能为空");
        }
        TodoTask existing = todoTaskMapper.selectById(taskId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        TodoTask update = new TodoTask();
        update.setTaskId(taskId);
        update.setDeleted(1);
        update.setUpdateTime(LocalDateTime.now());
        return todoTaskMapper.updateById(update) > 0;
    }

    @Override
    public List<TodoTask> listScheduleTasks(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "时间范围不能为空");
        }
        return todoTaskMapper.selectAdminScheduleTasks(buildQueryTask(start, end));
    }

    @Override
    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> stats = new HashMap<>();
        LambdaQueryWrapper<TodoTask> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TodoTask::getDeleted, 0);
        List<TodoTask> allTasks = todoTaskMapper.selectList(queryWrapper);
        long total = allTasks.size();
        long completed = allTasks.stream().filter(t -> t.getCompleted() != null && t.getCompleted() == 1).count();
        long pending = total - completed;
        long overdue = allTasks.stream()
                .filter(t -> t.getCompleted() == null || t.getCompleted() == 0)
                .filter(t -> t.getDueDate() != null && t.getDueDate().isBefore(LocalDateTime.now()))
                .count();
        stats.put("total", total);
        stats.put("completed", completed);
        stats.put("pending", pending);
        stats.put("overdue", overdue);
        return stats;
    }

    private void validateTask(TodoTask task) {
        if (StringUtils.isEmpty(task.getTaskTitle())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务标题不能为空");
        }
    }

    private TodoTask buildQueryTask(LocalDateTime start, LocalDateTime end) {
        TodoTask task = new TodoTask();
        task.setStartTime(start);
        task.setDueDate(end);
        task.setPageNum(1);
        task.setPageSize(Integer.MAX_VALUE);
        return task;
    }
}
