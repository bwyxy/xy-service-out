package com.xy.app.service;

import com.xy.app.pojo.dto.TodoTaskArchiveDTO;
import com.xy.app.pojo.dto.TodoTaskCreateDTO;
import com.xy.app.pojo.dto.TodoTaskQueryDTO;
import com.xy.app.pojo.dto.TodoTaskStatusDTO;
import com.xy.app.pojo.dto.TodoTaskUpdateDTO;
import com.xy.app.pojo.vo.TodoTagStatVO;
import com.xy.app.pojo.vo.TodoMonthVO;
import com.xy.app.pojo.vo.TodoQuadrantVO;
import com.xy.app.pojo.vo.TodoTaskCreateVO;
import com.xy.app.pojo.vo.TodoTaskDetailVO;
import com.xy.app.pojo.vo.TodoTaskPageVO;
import com.xy.app.pojo.vo.TodoTaskStatsVO;
import com.xy.app.pojo.vo.TodoWeekStatsVO;

import java.util.List;

/**
 * 客户端待办任务服务
 * */
public interface ITaskService {

    /**
     * 创建任务（按 clientId 幂等）
     * */
    TodoTaskCreateVO createTask(TodoTaskCreateDTO dto);

    /**
     * 任务分页列表（archived 传 0 查未归档、传 1 查已归档；tagId 为空表示不限清单）
     * */
    TodoTaskPageVO listTasks(TodoTaskQueryDTO dto);

    /**
     * 标记任务完成 / 取消完成（按目标状态写入，重复提交同一状态不报错）
     * */
    void completeTask(TodoTaskStatusDTO dto);

    /**
     * 归档 / 取消归档（按目标状态写入，只有已完成的任务能归档）
     * */
    void archiveTask(TodoTaskArchiveDTO dto);

    /**
     * 清空已完成任务（逻辑删除未归档的已完成任务）
     *
     * @return 清空的条数
     * */
    int clearDoneTasks();

    /**
     * 我的页统计：累计完成、连续记录天数、清单数
     * */
    TodoTaskStatsVO getTaskStats();

    /**
     * 统计页 · 本周统计：每日完成数、本周完成总数、完成率、连续记录天数
     *
     * @param weekStart 周起始 0-周日 1-周一，null 按周一开始
     * */
    TodoWeekStatsVO getWeekStats(Integer weekStart);

    /**
     * 统计页 · 清单分布：各清单下的任务数（含未归类），按数量倒序
     * */
    List<TodoTagStatVO> getTagDistribution();

    /**
     * 日历视图 · 按月统计：本月有任务的日期与当日条数
     *
     * @param month 月份 yyyy-MM
     * */
    TodoMonthVO getMonthStats(String month);

    /**
     * 四象限：按紧急程度分四格返回待完成任务（每格带前几条预览）
     * */
    TodoQuadrantVO getQuadrantGroups();

    /**
     * 任务详情（含清单名、创建时间与下次提醒时间）
     * */
    TodoTaskDetailVO getTaskDetail(Long taskId);

    /**
     * 更新任务（全量覆盖：没传的字段按清空处理）
     * */
    void updateTask(Long taskId, TodoTaskUpdateDTO dto);

    /**
     * 删除任务（逻辑删除，任务不再出现在任何列表里）
     * */
    void removeTask(Long taskId);
}
