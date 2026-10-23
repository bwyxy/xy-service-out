package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.app.pojo.vo.TodoDayCountVO;
import com.xy.app.pojo.vo.TodoQuadrantGroupVO;
import com.xy.app.pojo.vo.TodoTagStatVO;
import com.xy.app.pojo.vo.TodoTagVO;
import com.xy.app.pojo.vo.TodoTaskCountVO;
import com.xy.common.entity.todo.TodoTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 客户端待办任务 Mapper（表 t_todo_task）
 *
 * <p>类名不带 Todo 前缀，是为了避开 xy-system 里管理端的 {@code TodoTaskMapper}（表 todo_task）——
 * xy-app 的 {@code @MapperScan("com.xy.*.mapper")} 会一并扫到 {@code com.xy.system.mapper.todo}，
 * 同名接口会按简单类名注册出同名 Bean。</p>
 *
 * <p>手写 SQL 不受 MyBatis-Plus 逻辑删除影响，条件里必须自己带 {@code deleted = 0}。</p>
 * */
@Mapper
public interface TaskMapper extends BaseMapper<TodoTask> {

    /**
     * 按清单统计待完成任务数（清单列表的「N 件待办」用）
     *
     * <p>复用 {@link TodoTagVO} 承接结果，只回填 tagId / pendingCount 两个字段。</p>
     * */
    @Select("select tag_id as tagId, count(*) as pendingCount from t_todo_task "
            + "where user_id = #{userId} and deleted = 0 and completed = 0 and tag_id is not null "
            + "group by tag_id")
    List<TodoTagVO> countPendingGroupByTag(@Param("userId") Long userId);

    /**
     * 按紧急程度统计待完成任务数（四象限页）
     *
     * <p>复用 {@link TodoQuadrantGroupVO} 承接结果，只回填 urgencyLevel / total 两个字段；
     * 只算未完成、未归档的现存任务，已完成的从四象限里摘出去。</p>
     * */
    @Select("select urgency_level as urgencyLevel, count(*) as total from t_todo_task "
            + "where user_id = #{userId} and deleted = 0 and archived = 0 and completed = 0 "
            + "group by urgency_level")
    List<TodoQuadrantGroupVO> countPendingGroupByUrgency(@Param("userId") Long userId);

    /**
     * 按完成状态统计任务数（列表分页后前端算不出「全部/待完成/已完成」条数，由后端统计）
     *
     * <p>tagId 传 null 表示不限清单；keyword 传 null 表示不按关键词过滤（标题或备注命中即可）；
     * startTime / endTime 传 null 表示不限到期日，否则按 [startTime, endTime) 左闭右开区间过滤；
     * urgencyLevel 传 null 表示不限紧急程度，否则只统计该象限的任务；
     * archived 传 0 统计未归档任务，传 1 统计已归档任务。</p>
     * */
    @Select("select completed as completed, count(*) as total from t_todo_task "
            + "where user_id = #{userId} and deleted = 0 and archived = #{archived} "
            + "and (#{tagId} is null or tag_id = #{tagId}) "
            + "and (#{urgencyLevel} is null or urgency_level = #{urgencyLevel}) "
            + "and (#{keyword} is null or task_title like concat('%', #{keyword}, '%') "
            + "     or description like concat('%', #{keyword}, '%')) "
            + "and (#{startTime} is null or (due_date >= #{startTime} and due_date < #{endTime})) "
            + "group by completed")
    List<TodoTaskCountVO> countGroupByCompleted(@Param("userId") Long userId,
                                                @Param("tagId") Long tagId,
                                                @Param("keyword") String keyword,
                                                @Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime,
                                                @Param("urgencyLevel") Integer urgencyLevel,
                                                @Param("archived") int archived);

    /**
     * 有完成记录的日期（倒序），用于算「连续记录」天数
     *
     * <p>刻意不过滤 deleted：清空已完成只是「不再显示」，不该把连续记录打断。
     * since 限定回溯窗口，避免一次把多年记录全查出来（配合 idx_user_completed_time）。</p>
     * */
    @Select("select distinct date(completion_time) as doneDate from t_todo_task "
            + "where user_id = #{userId} and completed = 1 and completion_time is not null "
            + "and completion_time >= #{since} order by doneDate desc")
    List<LocalDate> selectCompletedDates(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * 区间内每天的完成数（统计页柱状图）
     *
     * <p>按 date(completion_time) 聚合，缺的日子由服务端补 0；
     * 刻意不过滤 deleted：清空已完成只是「不再显示」，不该让柱子事后变矮。</p>
     * */
    @Select("select date(completion_time) as `date`, count(*) as total from t_todo_task "
            + "where user_id = #{userId} and completed = 1 "
            + "and completion_time >= #{startTime} and completion_time < #{endTime} "
            + "group by `date`")
    List<TodoDayCountVO> selectDailyCompleted(@Param("userId") Long userId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 区间内每天到期的任务数（日历视图的点阵）
     *
     * <p>按 date(due_date) 聚合，只算未归档的现存任务，与首页、清单页的口径一致；
     * 左闭右开区间，避免 23:59:59 的毫秒边界坑。</p>
     * */
    @Select("select date(due_date) as `date`, count(*) as total from t_todo_task "
            + "where user_id = #{userId} and deleted = 0 and archived = 0 "
            + "and due_date >= #{startTime} and due_date < #{endTime} "
            + "group by `date` order by `date`")
    List<TodoDayCountVO> selectDailyDueCount(@Param("userId") Long userId,
                                             @Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime);

    /**
     * 区间内「经手」的任务数：区间内完成的 + 区间内到期且还没完成的（完成率的分母）
     *
     * <p>分母把分子整段包含在内，所以完成率天然不会超过 100%。
     * 完成那半边不带 deleted（与柱状图口径一致），未完成那半边必须是未删除的现存任务。</p>
     * */
    @Select("select count(*) from t_todo_task "
            + "where user_id = #{userId} "
            + "and ((completed = 1 and completion_time >= #{startTime} and completion_time < #{endTime}) "
            + "  or (completed = 0 and deleted = 0 "
            + "      and due_date >= #{startTime} and due_date < #{endTime}))")
    long countWeekScope(@Param("userId") Long userId,
                        @Param("startTime") LocalDateTime startTime,
                        @Param("endTime") LocalDateTime endTime);

    /**
     * 清单分布：各清单下的任务数（含未归类一档），按数量倒序
     *
     * <p>只算未删除的任务（含已完成与已归档）：分布看的是现在手上有哪些事。</p>
     * */
    @Select("select k.tag_id as tagId, coalesce(g.tag_name, '未归类') as tagName, "
            + "coalesce(g.color, '') as color, count(*) as total "
            + "from t_todo_task k left join t_todo_tag g on g.tag_id = k.tag_id "
            + "where k.user_id = #{userId} and k.deleted = 0 "
            + "group by k.tag_id, g.tag_name, g.color "
            + "order by total desc, k.tag_id asc")
    List<TodoTagStatVO> selectTagDistribution(@Param("userId") Long userId);

    /**
     * 把某个清单下的任务改为未归类（删除清单时调用，任务本身保留）
     * */
    @Update("update t_todo_task set tag_id = null, update_time = now() "
            + "where user_id = #{userId} and tag_id = #{tagId} and deleted = 0")
    int detachTag(@Param("userId") Long userId, @Param("tagId") Long tagId);

    /**
     * 清空已完成任务（逻辑删除，已完成与归档页的「清空已完成」用）
     *
     * <p>只清未归档的：已归档的那些还在「已归档」分组里留着，不属于「已完成」列表。</p>
     * */
    @Update("update t_todo_task set deleted = 1, update_time = now() "
            + "where user_id = #{userId} and deleted = 0 and archived = 0 and completed = 1")
    int clearCompleted(@Param("userId") Long userId);

    /**
     * 批量自动归档：完成时间早于 deadline 的未归档已完成任务全部置为已归档
     *
     * <p>走 idx_archived_completion(archived, completed, completion_time)，避免每小时全表扫。</p>
     * */
    @Update("update t_todo_task set archived = 1, archive_time = now(), update_time = now() "
            + "where deleted = 0 and archived = 0 and completed = 1 "
            + "and completion_time is not null and completion_time < #{deadline}")
    int archiveCompletedBefore(@Param("deadline") LocalDateTime deadline);
}
