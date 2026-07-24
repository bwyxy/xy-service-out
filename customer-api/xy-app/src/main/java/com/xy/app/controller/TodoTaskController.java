package com.xy.app.controller;

import com.xy.app.service.ITodoTaskService;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.dto.ToDoTaskDTO;
import com.xy.common.entity.TodoTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/task")
public class TodoTaskController {

    @Autowired
    private ITodoTaskService todoTaskService;

    /**
     * 根据传入时间查询当天任务
     * */
    @PostMapping("/day")
    public R<?> getCurrentDayTasks(@RequestBody TodoTask todoTask) {

        PageResult<TodoTask> pageResult = todoTaskService.selectTaskByDate(todoTask);

        return R.ok(pageResult);
    }

    /**
     * 查询任务列表
     * */
    @PostMapping("/list")
    public R<?> selectTaskList(@RequestBody TodoTask xyTask) {

        PageResult<TodoTask> pageResult = todoTaskService.selectTaskList(xyTask);

        return R.ok(pageResult);
    }

    /**
     * 根据任务 ID查询任务信息
     * */
    @GetMapping("/{id}")
    public R<TodoTask> selectTaskById(@PathVariable Long id) {

        return R.ok(todoTaskService.selectTaskById(id));
    }

    /**
     * 创建任务
     * */
    @PostMapping("/create")
    public R<?> createTask(@RequestBody ToDoTaskDTO TodoTaskDTO) {

        todoTaskService.createTask(TodoTaskDTO);

        return R.ok("创建成功");
    }

    /**
     * 更新任务
     * */
    @PostMapping("/update")
    public R<?> updateTask(@RequestBody ToDoTaskDTO TodoTaskDTO) {
        todoTaskService.updateTask(TodoTaskDTO);

        return R.ok("更新成功");
    }

    /**
     * 设置任务完成状态
     * */
    @PostMapping("/completed")
    public R<?> completedTask(@RequestBody TodoTask xyTask) {

        todoTaskService.setTaskCompleted(xyTask);

        return R.ok("设置任务状态成功");
    }

    /**
     * 删除任务
     * */
    @DeleteMapping("/{id}")
    public R<?> deleteTask(@PathVariable Long id) {

        todoTaskService.deleteTaskByTaskId(id);

        return R.ok("删除成功");
    }
}
