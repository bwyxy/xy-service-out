package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.HabitLogMapper;
import com.xy.app.mapper.HabitMapper;
import com.xy.app.pojo.dto.TodoHabitCheckDTO;
import com.xy.app.pojo.dto.TodoHabitSaveDTO;
import com.xy.app.pojo.vo.TodoHabitVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.IHabitService;
import com.xy.common.entity.todo.TodoHabit;
import com.xy.common.entity.todo.TodoHabitLog;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.ContentCheckUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 客户端习惯服务实现
 * */
@Service
@RequiredArgsConstructor
public class HabitServiceImpl extends ServiceImpl<HabitMapper, TodoHabit> implements IHabitService {

    /** 习惯名称长度上限（对齐 t_todo_habit.name varchar(64)） */
    private static final int HABIT_NAME_MAX_LENGTH = 64;

    /** 每天打卡 */
    private static final String FREQ_TYPE_DAILY = "daily";

    /** 按周打卡 */
    private static final String FREQ_TYPE_WEEKLY = "weekly";

    /** 一周的天数，也是本周打卡格子的长度（索引 0 为周一） */
    private static final int WEEK_DAYS = 7;

    /** 连续天数回溯上限（约十年），避免一次把多年记录全查出来 */
    private static final int STREAK_LOOKBACK_DAYS = 3650;

    /** 星期取值：1-周一 ~ 7-周日 */
    private static final int MIN_WEEK_DAY = 1;
    private static final int MAX_WEEK_DAY = 7;

    private final HabitLogMapper habitLogMapper;

    @Override
    public List<TodoHabitVO> listHabits() {
        Long uId = TodoUserDetails.getCurrentUId();

        List<TodoHabit> habits = list(new LambdaQueryWrapper<TodoHabit>()
                .eq(TodoHabit::getUserId, uId)
                .eq(TodoHabit::getStatus, 1)
                .orderByAsc(TodoHabit::getSortNo)
                .orderByAsc(TodoHabit::getHabitId));

        if (habits.isEmpty()) {
            return List.of();
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);

        // 一次把回溯窗口内的打卡日期全查出来，在内存里算连续天数与本周格子，避免每个习惯各查一次
        Map<Long, Set<LocalDate>> checkedMap = toCheckedMap(uId, null, today);

        return habits.stream()
                .map(habit -> toVO(habit, checkedMap, today, weekStart))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoHabitVO createHabit(TodoHabitSaveDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();

        String freqType = checkFreqType(dto.getFreqType());

        TodoHabit habit = new TodoHabit();
        habit.setUserId(uId);
        habit.setName(checkName(dto.getName()));
        habit.setIcon(text(dto.getIcon()));
        habit.setColor(text(dto.getColor()));
        habit.setFreqType(freqType);
        habit.setFreqDays(checkFreqDays(freqType, dto.getFreqDays()));
        habit.setTargetDays(normalizeTargetDays(dto.getTargetDays()));
        habit.setSortNo(dto.getSortNo() == null ? nextSortNo(uId) : dto.getSortNo());
        habit.setStatus(1);
        habit.setDeleted(0);
        habit.setCreateTime(LocalDateTime.now());

        save(habit);

        // 刚建的习惯当天还没打卡，连续天数为 0，本周格子全空
        return toVO(habit, Map.of(), LocalDate.now(), LocalDate.now().with(DayOfWeek.MONDAY));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateHabit(Long habitId, TodoHabitSaveDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();

        TodoHabit exist = getOwnHabit(uId, habitId);
        String freqType = checkFreqType(dto.getFreqType());

        TodoHabit update = new TodoHabit();
        update.setName(checkName(dto.getName()));
        update.setIcon(dto.getIcon() == null ? exist.getIcon() : text(dto.getIcon()));
        update.setColor(dto.getColor() == null ? exist.getColor() : text(dto.getColor()));
        update.setFreqType(freqType);
        update.setFreqDays(checkFreqDays(freqType, dto.getFreqDays()));
        update.setTargetDays(normalizeTargetDays(dto.getTargetDays()));
        update.setSortNo(dto.getSortNo() == null ? exist.getSortNo() : dto.getSortNo());

        update(update, new LambdaUpdateWrapper<TodoHabit>()
                .eq(TodoHabit::getHabitId, habitId)
                .eq(TodoHabit::getUserId, uId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeHabit(Long habitId) {
        Long uId = TodoUserDetails.getCurrentUId();

        getOwnHabit(uId, habitId);

        // 只逻辑删除习惯本身，历史打卡记录保留，避免误删后统计对不上
        remove(new LambdaQueryWrapper<TodoHabit>()
                .eq(TodoHabit::getHabitId, habitId)
                .eq(TodoHabit::getUserId, uId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoHabitVO checkHabit(Long habitId, TodoHabitCheckDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();

        TodoHabit habit = getOwnHabit(uId, habitId);

        LocalDate today = LocalDate.now();
        LocalDate logDate = parseLogDate(dto == null ? null : dto.getLogDate());

        if (logDate.isAfter(today)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "还没到那一天，先别急着打卡");
        }

        // uk_habit_date(habit_id, log_date) 保证同一天只有一条，存在即取消打卡
        TodoHabitLog exist = habitLogMapper.selectOne(new LambdaQueryWrapper<TodoHabitLog>()
                .eq(TodoHabitLog::getHabitId, habitId)
                .eq(TodoHabitLog::getUserId, uId)
                .eq(TodoHabitLog::getLogDate, logDate));

        if (exist == null) {
            TodoHabitLog log = new TodoHabitLog();
            log.setHabitId(habitId);
            log.setUserId(uId);
            log.setLogDate(logDate);
            log.setStatus(1);
            log.setCreateTime(LocalDateTime.now());

            habitLogMapper.insert(log);
        } else {
            habitLogMapper.deleteById(exist.getId());
        }

        // 回该习惯的最新状态，前端只刷新这一行，不必整表重拉
        return toVO(habit, toCheckedMap(uId, habitId, today), today, today.with(DayOfWeek.MONDAY));
    }

    /**
     * 取当前用户自己的习惯，不存在直接报错（顺带挡住越权改别人的习惯）
     * */
    private TodoHabit getOwnHabit(Long uId, Long habitId) {
        if (habitId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "习惯 Id 不能为空");
        }

        TodoHabit habit = getOne(new LambdaQueryWrapper<TodoHabit>()
                .eq(TodoHabit::getHabitId, habitId)
                .eq(TodoHabit::getUserId, uId), false);

        if (habit == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "习惯不存在");
        }

        return habit;
    }

    /**
     * 查回溯窗口内的打卡日期，按习惯 Id 分组
     * */
    private Map<Long, Set<LocalDate>> toCheckedMap(Long uId, Long habitId, LocalDate today) {
        return habitLogMapper.selectCheckedLogs(uId, habitId, today.minusDays(STREAK_LOOKBACK_DAYS))
                .stream()
                .filter(row -> row.getHabitId() != null && row.getLogDate() != null)
                .collect(Collectors.groupingBy(TodoHabitLog::getHabitId,
                        Collectors.mapping(TodoHabitLog::getLogDate, Collectors.toSet())));
    }

    /**
     * 校验习惯名（去空格、长度、敏感词）
     * */
    private String checkName(String rawName) {
        String name = rawName == null ? null : rawName.trim();
        ContentCheckUtils.check(name, 1, HABIT_NAME_MAX_LENGTH);

        return name;
    }

    /**
     * 校验频率类型，不传按每天算
     * */
    private String checkFreqType(String rawFreqType) {
        if (rawFreqType == null || rawFreqType.isBlank()) {
            return FREQ_TYPE_DAILY;
        }

        if (!FREQ_TYPE_DAILY.equals(rawFreqType) && !FREQ_TYPE_WEEKLY.equals(rawFreqType)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "频率类型只能是 daily 或 weekly");
        }

        return rawFreqType;
    }

    /**
     * 校验并拼装星期集合：daily 存空串清掉上一次的选择，weekly 至少要选一天
     * */
    private String checkFreqDays(String freqType, List<Integer> freqDays) {
        if (!FREQ_TYPE_WEEKLY.equals(freqType)) {
            return "";
        }

        String days = (freqDays == null ? List.<Integer>of() : freqDays).stream()
                .filter(Objects::nonNull)
                .filter(day -> day >= MIN_WEEK_DAY && day <= MAX_WEEK_DAY)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        if (days.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "按周打卡请至少选择一天");
        }

        return days;
    }

    /**
     * 目标天数：不传或负数都按「不限」处理
     * */
    private int normalizeTargetDays(Integer targetDays) {
        return targetDays == null || targetDays < 0 ? 0 : targetDays;
    }

    /**
     * 解析打卡日期，不传按今天算
     * */
    private LocalDate parseLogDate(String rawLogDate) {
        if (rawLogDate == null || rawLogDate.isBlank()) {
            return LocalDate.now();
        }

        try {
            return LocalDate.parse(rawLogDate.trim());
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "打卡日期格式应为 yyyy-MM-dd");
        }
    }

    /**
     * 取当前用户最大排序 + 1，新习惯排到末尾
     * */
    private int nextSortNo(Long uId) {
        TodoHabit last = getOne(new LambdaQueryWrapper<TodoHabit>()
                .eq(TodoHabit::getUserId, uId)
                .orderByDesc(TodoHabit::getSortNo)
                .last("limit 1"), false);

        return last == null || last.getSortNo() == null ? 1 : last.getSortNo() + 1;
    }

    /**
     * 连续打卡天数：从今天起往前数连续有打卡记录的自然日；今天还没打卡不算断，从昨天起数
     * */
    private int calcStreak(Set<LocalDate> checkedDates, LocalDate today) {
        LocalDate cursor = checkedDates.contains(today) ? today : today.minusDays(1);
        int streak = 0;

        while (checkedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    /**
     * 本周打卡格子，索引 0 为周一
     * */
    private List<Boolean> weekCells(Set<LocalDate> checkedDates, LocalDate weekStart) {
        List<Boolean> cells = new ArrayList<>(WEEK_DAYS);

        for (int i = 0; i < WEEK_DAYS; i++) {
            cells.add(checkedDates.contains(weekStart.plusDays(i)));
        }

        return cells;
    }

    /**
     * 把库里存的 "1,3,5" 拆成星期集合
     * */
    private List<Integer> parseFreqDays(String freqDays) {
        if (freqDays == null || freqDays.isBlank()) {
            return List.of();
        }

        return Arrays.stream(freqDays.split(","))
                .map(String::trim)
                .filter(day -> day.matches("[1-7]"))
                .map(Integer::valueOf)
                .distinct()
                .sorted()
                .toList();
    }

    private TodoHabitVO toVO(TodoHabit habit, Map<Long, Set<LocalDate>> checkedMap,
                             LocalDate today, LocalDate weekStart) {
        Set<LocalDate> checkedDates = checkedMap.getOrDefault(habit.getHabitId(), Set.of());

        TodoHabitVO vo = new TodoHabitVO();
        vo.setHabitId(habit.getHabitId());
        vo.setName(habit.getName());
        vo.setIcon(habit.getIcon());
        vo.setColor(habit.getColor());
        vo.setFreqType(habit.getFreqType());
        vo.setFreqDays(parseFreqDays(habit.getFreqDays()));
        vo.setTargetDays(habit.getTargetDays());
        vo.setSortNo(habit.getSortNo());
        vo.setStatus(habit.getStatus());
        vo.setStreak(calcStreak(checkedDates, today));
        vo.setWeekDays(weekCells(checkedDates, weekStart));
        vo.setCreateTime(habit.getCreateTime());

        return vo;
    }

    private String text(String value) {
        return value == null ? "" : value;
    }
}
