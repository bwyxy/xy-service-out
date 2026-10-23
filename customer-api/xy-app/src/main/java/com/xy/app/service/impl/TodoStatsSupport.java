package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xy.app.mapper.TagMapper;
import com.xy.app.mapper.TaskMapper;
import com.xy.app.pojo.vo.TodoDayCountVO;
import com.xy.app.pojo.vo.TodoMonthVO;
import com.xy.app.pojo.vo.TodoQuadrantGroupVO;
import com.xy.app.pojo.vo.TodoQuadrantVO;
import com.xy.app.pojo.vo.TodoTagStatVO;
import com.xy.app.pojo.vo.TodoTaskStatsVO;
import com.xy.app.pojo.vo.TodoWeekStatsVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.support.TodoTaskConverter;
import com.xy.common.entity.todo.TodoTag;
import com.xy.common.entity.todo.TodoTask;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 待办任务统计：我的页统计、本周统计、清单分布、日历按月统计、四象限分组
 * */
@Component
@RequiredArgsConstructor
public class TodoStatsSupport {

    /** 四象限的紧急程度档位，顺序按「该先做什么」排：重要且紧急 → 重要不紧急 → 紧急不重要 → 不重要不紧急 */
    private static final int[] QUADRANT_LEVELS = {4, 3, 2, 1};

    /** 四象限每格带几条预览（格子里放不下的走列表接口按象限翻页看） */
    private static final int QUADRANT_PREVIEW_LIMIT = 3;

    /** 算连续记录天数时最多往前回溯多少天（按每天算约 10 年），防一次捞出全部历史 */
    private static final int MAX_STREAK_LOOKBACK_DAYS = 3650;

    /** 一周七天 */
    private static final int DAYS_OF_WEEK = 7;

    private final TaskMapper taskMapper;

    private final TagMapper tagMapper;

    /**
     * 我的页统计：累计完成、连续记录天数、清单数
     * */
    public TodoTaskStatsVO getTaskStats() {

        Long uId = TodoUserDetails.getCurrentUId();

        TodoTaskStatsVO vo = new TodoTaskStatsVO();
        // 累计完成刻意不过滤 deleted：清空已完成只是「不再显示」，累计成绩不该被清空抹掉；
        // 取消完成会把 completed 改回 0，所以点错撤销的不算
        vo.setDoneTotal(taskMapper.selectCount(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, uId)
                .eq(TodoTask::getCompleted, 1L)));
        vo.setStreakDays(calcStreakDays(uId));
        // 清单数按清单列表页的口径来：不算已归档的（那页也只列未归档的）
        vo.setTagCount(tagMapper.selectCount(new LambdaQueryWrapper<TodoTag>()
                .eq(TodoTag::getUserId, uId)
                .eq(TodoTag::getArchived, 0)));

        return vo;
    }

    /**
     * 本周统计：每日完成数、本周完成总数、完成率、连续记录天数
     * */
    public TodoWeekStatsVO getWeekStats(Integer weekStart) {

        Long uId = TodoUserDetails.getCurrentUId();

        LocalDate today = LocalDate.now();
        // 周起始跟首页周视图保持一致：0-周日，其余（不传或传 1）按周一；DayOfWeek 是周一=1…周日=7
        int dayOfWeek = today.getDayOfWeek().getValue();
        LocalDate startDate = today.minusDays(weekStart != null && weekStart == 0 ? dayOfWeek % 7 : dayOfWeek - 1L);
        LocalDate endDate = startDate.plusDays(DAYS_OF_WEEK - 1L);
        LocalDateTime startTime = startDate.atStartOfDay();
        LocalDateTime endTime = endDate.plusDays(1).atStartOfDay();

        // 先把有完成记录的日期收进 map，再按 7 天铺开，没完成的日子补 0（柱状图要连续的 7 根）
        Map<LocalDate, Long> daily = new HashMap<>();
        for (TodoDayCountVO row : taskMapper.selectDailyCompleted(uId, startTime, endTime)) {
            if (row.getDate() != null) {
                daily.put(row.getDate(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }

        List<TodoDayCountVO> days = new ArrayList<>(DAYS_OF_WEEK);
        long total = 0L;

        for (int i = 0; i < DAYS_OF_WEEK; i++) {
            LocalDate day = startDate.plusDays(i);
            long count = daily.getOrDefault(day, 0L);

            TodoDayCountVO item = new TodoDayCountVO();
            item.setDate(day);
            item.setTotal(count);
            days.add(item);

            total += count;
        }

        // 完成率的分母是「本周经手」的任务数，把分子整段包含在内，所以不会超过 100%
        long scope = taskMapper.countWeekScope(uId, startTime, endTime);

        TodoWeekStatsVO vo = new TodoWeekStatsVO();
        vo.setStartDate(startDate);
        vo.setEndDate(endDate);
        vo.setDays(days);
        vo.setTotal(total);
        vo.setRate(scope <= 0 ? 0 : Math.round(total * 100f / scope));
        vo.setStreakDays(calcStreakDays(uId));

        return vo;
    }

    /**
     * 清单分布：各清单下的任务数（含未归类），按数量倒序
     * */
    public List<TodoTagStatVO> getTagDistribution() {
        return taskMapper.selectTagDistribution(TodoUserDetails.getCurrentUId());
    }

    /**
     * 日历按月统计：本月有任务的日期与当日条数
     * */
    public TodoMonthVO getMonthStats(String month) {

        Long uId = TodoUserDetails.getCurrentUId();

        YearMonth yearMonth;

        try {
            yearMonth = YearMonth.parse(month == null ? "" : month.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "月份格式不正确，应为 yyyy-MM");
        }

        // 整月左闭右开：1 号 00:00:00 ~ 下月 1 号 00:00:00
        LocalDateTime startTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endTime = yearMonth.plusMonths(1).atDay(1).atStartOfDay();

        // 没有任务的日子不返回，点阵只点亮有任务的那几天；有任务的天数就是返回的条数
        List<TodoDayCountVO> days = taskMapper.selectDailyDueCount(uId, startTime, endTime);

        TodoMonthVO vo = new TodoMonthVO();
        vo.setYear(yearMonth.getYear());
        vo.setMonth(yearMonth.getMonthValue());
        vo.setBusyDays(days.size());
        vo.setDays(days);

        return vo;
    }

    /**
     * 四象限：按紧急程度分四格返回待完成任务（每格带前几条预览）
     * */
    public TodoQuadrantVO getQuadrantGroups() {

        Long uId = TodoUserDetails.getCurrentUId();

        // 计数一次 group by 拿全，再按固定的四档兜出没数据的那几格，前端不用自己补齐
        Map<Integer, Long> counts = new HashMap<>();
        for (TodoQuadrantGroupVO row : taskMapper.countPendingGroupByUrgency(uId)) {
            if (row.getUrgencyLevel() != null) {
                counts.put(row.getUrgencyLevel(), row.getTotal() == null ? 0L : row.getTotal());
            }
        }

        List<TodoQuadrantGroupVO> groups = new ArrayList<>(QUADRANT_LEVELS.length);

        for (int level : QUADRANT_LEVELS) {
            long total = counts.getOrDefault(level, 0L);

            TodoQuadrantGroupVO group = new TodoQuadrantGroupVO();
            group.setUrgencyLevel(level);
            group.setTotal(total);
            // 预览按主线列表同一套排序（没设日期的最后），只是多一个 limit
            group.setTasks(total <= 0 ? List.of() : taskMapper.selectList(new LambdaQueryWrapper<TodoTask>()
                    .eq(TodoTask::getUserId, uId)
                    .eq(TodoTask::getDeleted, 0)
                    .eq(TodoTask::getArchived, 0)
                    .eq(TodoTask::getCompleted, 0L)
                    .eq(TodoTask::getUrgencyLevel, level)
                    .last("order by due_date is null asc, due_date asc, create_time desc limit "
                            + QUADRANT_PREVIEW_LIMIT))
                    .stream()
                    .map(TodoTaskConverter::toListVO)
                    .toList());
            groups.add(group);
        }

        TodoQuadrantVO vo = new TodoQuadrantVO();
        vo.setGroups(groups);

        return vo;
    }

    /**
     * 连续记录天数
     *
     * <p>今天有完成就从今天起数，今天还没完成就从昨天起数（当天还没做完不算断）；
     * 昨天也没有就是 0。只回溯有限窗口，超长连续记录按窗口上限截断。</p>
     * */
    private int calcStreakDays(Long uId) {

        LocalDate today = LocalDate.now();
        Set<LocalDate> doneDays = new HashSet<>(taskMapper.selectCompletedDates(uId,
                today.minusDays(MAX_STREAK_LOOKBACK_DAYS).atStartOfDay()));

        LocalDate cursor = doneDays.contains(today) ? today : today.minusDays(1);

        int streak = 0;

        while (doneDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }
}
