package com.xy.app.controller;

import com.xy.app.pojo.dto.TodoMonthQueryDTO;
import com.xy.app.pojo.dto.TodoTaskArchiveDTO;
import com.xy.app.pojo.dto.TodoTaskCreateDTO;
import com.xy.app.pojo.dto.TodoTaskQueryDTO;
import com.xy.app.pojo.dto.TodoTaskStatusDTO;
import com.xy.app.pojo.dto.TodoTaskUpdateDTO;
import com.xy.app.pojo.vo.TodoMonthVO;
import com.xy.app.pojo.vo.TodoQuadrantVO;
import com.xy.app.pojo.vo.TodoTaskCreateVO;
import com.xy.app.pojo.vo.TodoTaskDetailVO;
import com.xy.app.pojo.vo.TodoTaskPageVO;
import com.xy.app.pojo.vo.TodoTaskStatsVO;
import com.xy.app.service.ITaskService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端待办任务
 * */
@RestController
@RequestMapping("/task")
@RequiredArgsConstructor
public class TaskController {

    private final ITaskService taskService;

    /**
     * 创建任务
     * */
    @PostMapping("/create")
    public R<TodoTaskCreateVO> create(@RequestBody TodoTaskCreateDTO dto) {
        return R.ok(taskService.createTask(dto));
    }

    /**
     * 任务分页列表（清单 + 状态筛选，附带清单维度的三个计数）
     * */
    @PostMapping("/list")
    public R<TodoTaskPageVO> list(@RequestBody(required = false) TodoTaskQueryDTO dto) {
        return R.ok(taskService.listTasks(dto));
    }

    /**
     * 任务详情（含清单名、创建时间与下次提醒时间）
     *
     * <p>路径变量匹配不会抢走 {@code /task/stats}：Spring 精确路径优先于变量路径。</p>
     * */
    @GetMapping("/{taskId}")
    public R<TodoTaskDetailVO> detail(@PathVariable Long taskId) {
        return R.ok(taskService.getTaskDetail(taskId));
    }

    /**
     * 标记任务完成 / 取消完成
     * */
    @PostMapping("/complete")
    public R<Void> complete(@RequestBody TodoTaskStatusDTO dto) {
        taskService.completeTask(dto);

        return R.ok();
    }

    /**
     * 我的页统计：累计完成、连续记录天数、清单数
     * */
    @GetMapping("/stats")
    public R<TodoTaskStatsVO> stats() {
        return R.ok(taskService.getTaskStats());
    }

    /**
     * 日历视图 · 按月统计（有任务的日期 + 当日条数）
     * */
    @PostMapping("/month")
    public R<TodoMonthVO> month(@RequestBody(required = false) TodoMonthQueryDTO dto) {
        return R.ok(taskService.getMonthStats(dto == null ? null : dto.getMonth()));
    }

    /**
     * 四象限：按紧急程度分四格返回待完成任务（每格带前几条预览）
     * */
    @PostMapping("/quadrant")
    public R<TodoQuadrantVO> quadrant() {
        return R.ok(taskService.getQuadrantGroups());
    }

    /**
     * 归档 / 取消归档（只有已完成的任务能归档）
     * */
    @PostMapping("/archive")
    public R<Void> archive(@RequestBody TodoTaskArchiveDTO dto) {
        taskService.archiveTask(dto);

        return R.ok();
    }

    /**
     * 清空已完成任务（逻辑删除未归档的已完成任务）
     * */
    @PostMapping("/clear-done")
    public R<Integer> clearDone() {
        return R.ok(taskService.clearDoneTasks());
    }

    /**
     * 更新任务（全量覆盖）
     * */
    @PutMapping("/{taskId}")
    public R<Void> update(@PathVariable Long taskId, @RequestBody TodoTaskUpdateDTO dto) {
        taskService.updateTask(taskId, dto);

        return R.ok();
    }

    /**
     * 删除任务（逻辑删除）
     * */
    @DeleteMapping("/{taskId}")
    public R<Void> remove(@PathVariable Long taskId) {
        taskService.removeTask(taskId);

        return R.ok();
    }
}
