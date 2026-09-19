package com.xy.admin.controller.todo;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.TodoTask;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.todo.ITodoTaskAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 待办任务管理
 * */
@RestController
@RequestMapping("/todo/task")
@RequiredArgsConstructor
public class TodoTaskAdminController {

    private final ITodoTaskAdminService todoTaskAdminService;

    /**
     * 查询待办任务列表
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:list')")
    @PostMapping("/list")
    public R<PageResult<TodoTask>> list(@RequestBody TodoTask task) {
        return R.ok(todoTaskAdminService.selectTaskList(task));
    }

    /**
     * 根据任务ID查询详情
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:query')")
    @GetMapping("/{taskId}")
    public R<TodoTask> getInfo(@PathVariable Long taskId) {
        return R.ok(todoTaskAdminService.selectTaskById(taskId));
    }

    /**
     * 新增待办任务
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:add')")
    @Log(title = "待办任务管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody TodoTask task) {
        return R.ok(todoTaskAdminService.addTask(task));
    }

    /**
     * 修改待办任务
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:edit')")
    @Log(title = "待办任务管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody TodoTask task) {
        todoTaskAdminService.updateTask(task);
        return R.ok();
    }

    /**
     * 删除待办任务
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:remove')")
    @Log(title = "待办任务管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{taskId}")
    public R<Void> remove(@PathVariable Long taskId) {
        todoTaskAdminService.deleteTask(taskId);
        return R.ok();
    }

    /**
     * 查询日程表任务（按时间范围）
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:list')")
    @GetMapping("/schedule")
    public R<?> schedule(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end) {
        return R.ok(todoTaskAdminService.listScheduleTasks(start, end));
    }

    /**
     * 仪表盘任务统计
     * */
    @PreAuthorize("@auth.hasPermi('todo:task:list')")
    @GetMapping("/statistics")
    public R<Map<String, Long>> statistics() {
        return R.ok(todoTaskAdminService.getDashboardStatistics());
    }
}
