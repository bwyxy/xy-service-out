package com.xy.admin.controller.todo;

import com.xy.common.core.domain.R;
import com.xy.common.entity.TodoTask;
import com.xy.system.service.todo.ITodoTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RequestMapping("/todo")
@RestController
public class TodoTaskController {

    @Autowired
    private ITodoTaskService todoTaskService;

    /**
     * 根据开始结束时间查询任务树
     * */
    @PostMapping("/task/tree")
    public R<List<TodoTask>> listTodoTasks(@RequestBody TodoTask todoTask) {

        List<TodoTask> todoTasks = todoTaskService.listPendingTasks(todoTask.getStartTime(), todoTask.getDueDate());

        return R.ok(todoTasks);
    }
}
