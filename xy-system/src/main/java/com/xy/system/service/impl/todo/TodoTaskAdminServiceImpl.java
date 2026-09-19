package com.xy.system.service.impl.todo;

import com.xy.common.core.domain.PageResult;
import com.xy.common.entity.TodoTask;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
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

        List<TodoTask> list = todoTaskMapper.selectAdminTaskList(task);
        Long total = todoTaskMapper.countAdminTaskList(task);

        int fromIndex = (pageNum - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, list.size());
        List<TodoTask> records = fromIndex >= list.size() ? List.of() : list.subList(fromIndex, toIndex);

        return PageResult.of(records, total, (long) pageNum, (long) pageSize);
    }

    @Override
    public TodoTask selectTaskById(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务ID不能为空");
        }
        return todoTaskMapper.selectAdminTaskById(taskId);
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
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        todoTaskMapper.insertAdminTask(task);
        return task.getTaskId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(TodoTask task) {
        if (task.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务ID不能为空");
        }
        validateTask(task);
        TodoTask existing = todoTaskMapper.selectAdminTaskById(task.getTaskId());
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        task.setUpdateTime(LocalDateTime.now());
        return todoTaskMapper.updateAdminTask(task) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteTask(Long taskId) {
        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务ID不能为空");
        }
        TodoTask existing = todoTaskMapper.selectAdminTaskById(taskId);
        if (existing == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "任务不存在");
        }
        return todoTaskMapper.deleteAdminTask(taskId) > 0;
    }

    @Override
    public List<TodoTask> listScheduleTasks(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "时间范围不能为空");
        }
        return todoTaskMapper.selectTaskByDate(buildQueryTask(start, end));
    }

    @Override
    public Map<String, Long> getDashboardStatistics() {
        Map<String, Long> stats = new HashMap<>();
        List<TodoTask> allTasks = todoTaskMapper.selectAdminTaskList(new TodoTask());
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
