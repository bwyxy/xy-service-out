package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoHabitLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 客户端习惯打卡记录 Mapper（表 t_todo_habit_log）
 *
 * <p>类名不带 Todo 前缀，是为了避开 xy-system 里的管理端同名 Mapper——理由同 {@link HabitMapper}。</p>
 * */
@Mapper
public interface HabitLogMapper extends BaseMapper<TodoHabitLog> {

    /**
     * 回溯窗口内的打卡日期（用于算连续天数与本周格子）
     *
     * <p>一次把窗口内的记录全查出来在内存里聚合，避免「每个习惯各查一次」的 N+1。
     * habitId 传 null 表示查该用户全部习惯（列表用），传值则只查某个习惯（打卡后只刷新一行用）；
     * since 是回溯下界，防止把多年记录一次拉出来。</p>
     * */
    @Select("select habit_id as habitId, log_date as logDate from t_todo_habit_log "
            + "where user_id = #{userId} and status = 1 and log_date >= #{since} "
            + "and (#{habitId} is null or habit_id = #{habitId}) "
            + "order by habit_id asc, log_date desc")
    List<TodoHabitLog> selectCheckedLogs(@Param("userId") Long userId,
                                         @Param("habitId") Long habitId,
                                         @Param("since") LocalDate since);
}
