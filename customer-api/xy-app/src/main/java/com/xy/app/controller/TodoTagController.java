package com.xy.app.controller;

import com.xy.app.service.ITodoTagService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.TodoTag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tag")
public class TodoTagController {

    @Autowired
    private ITodoTagService todoTagService;

    /**
     * 获取标签列表
     * */
    @PostMapping("/list")
    public R<?> list(@RequestBody TodoTag todoTag) {
        PageResult<TodoTag> pageResult = todoTagService.getTodoTags(todoTag);

        return R.ok(pageResult);
    }

    /**
     * 获取默认标签列表
     * */
    @Anonymous
    @PostMapping("/list/default")
    public R<?> listDefault() {
        List<TodoTag> list = todoTagService.getDefaultTodoTags();

        return R.ok(list);
    }

    /**
     * 添加标签
     * */
    @PostMapping("/add")
    public R<?> addTag(@RequestBody TodoTag todoTag) {
        todoTagService.addTodoTag(todoTag);

        return R.ok("添加成功");
    }

    /**
     * 删除标签
     * */
    @DeleteMapping("/{tagId}")
    public R<?> deleteTag(@PathVariable Long tagId) {
        todoTagService.removeTodoTag(tagId);

        return R.ok("删除成功");
    }
}
