package com.xy.admin.controller.todo;

import com.xy.common.annotation.Log;
import com.xy.common.core.domain.PageResult;
import com.xy.common.core.domain.R;
import com.xy.common.entity.TodoTag;
import com.xy.common.enums.BusinessType;
import com.xy.system.service.todo.ITodoTagAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 任务标签管理
 * */
@RestController
@RequestMapping("/todo/tag")
@RequiredArgsConstructor
public class TodoTagAdminController {

    private final ITodoTagAdminService todoTagAdminService;

    /**
     * 查询任务标签列表
     * */
    @PreAuthorize("@auth.hasPermi('todo:tag:list')")
    @PostMapping("/list")
    public R<PageResult<TodoTag>> list(@RequestBody TodoTag tag) {
        return R.ok(todoTagAdminService.selectTagList(tag));
    }

    /**
     * 根据标签ID查询详情
     * */
    @PreAuthorize("@auth.hasPermi('todo:tag:query')")
    @GetMapping("/{tagId}")
    public R<TodoTag> getInfo(@PathVariable Long tagId) {
        return R.ok(todoTagAdminService.selectTagById(tagId));
    }

    /**
     * 新增任务标签
     * */
    @PreAuthorize("@auth.hasPermi('todo:tag:add')")
    @Log(title = "任务标签管理", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@RequestBody TodoTag tag) {
        return R.ok(todoTagAdminService.addTag(tag));
    }

    /**
     * 修改任务标签
     * */
    @PreAuthorize("@auth.hasPermi('todo:tag:edit')")
    @Log(title = "任务标签管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@RequestBody TodoTag tag) {
        todoTagAdminService.updateTag(tag);
        return R.ok();
    }

    /**
     * 删除任务标签
     * */
    @PreAuthorize("@auth.hasPermi('todo:tag:remove')")
    @Log(title = "任务标签管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tagId}")
    public R<Void> remove(@PathVariable Long tagId) {
        todoTagAdminService.deleteTag(tagId);
        return R.ok();
    }
}
