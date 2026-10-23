package com.xy.app.controller;

import com.xy.app.pojo.vo.TodoTagStatVO;
import com.xy.app.pojo.vo.TodoWeekStatsVO;
import com.xy.app.service.ITaskService;
import com.xy.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户端待办统计
 * */
@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final ITaskService taskService;

    /**
     * 本周统计：每日完成数 + 本周完成总数 + 完成率 + 连续记录天数
     *
     * @param weekStart 周起始 0-周日 1-周一（默认），与前端外观设置里的周起始保持一致
     * */
    @GetMapping("/week")
    public R<TodoWeekStatsVO> week(@RequestParam(required = false) Integer weekStart) {
        return R.ok(taskService.getWeekStats(weekStart));
    }

    /**
     * 清单分布：各清单下的任务数（含未归类），按数量倒序
     * */
    @GetMapping("/distribution")
    public R<List<TodoTagStatVO>> distribution() {
        return R.ok(taskService.getTagDistribution());
    }
}
