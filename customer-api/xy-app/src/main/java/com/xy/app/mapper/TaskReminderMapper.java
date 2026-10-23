package com.xy.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xy.common.entity.todo.TodoTaskReminder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 客户端任务提醒 Mapper（表 t_todo_task_reminder）
 * */
@Mapper
public interface TaskReminderMapper extends BaseMapper<TodoTaskReminder> {

    /**
     * 作废某个任务下还没下发的提醒点（改任务时间时调用，避免调度侧拿到过期时间）
     *
     * <p>保留记录、只改状态，不物理删除：表里的 status 本来就设计了 canceled。</p>
     * */
    @Update("update t_todo_task_reminder set status = 'canceled', update_time = now() "
            + "where task_id = #{taskId} and user_id = #{userId} and status = 'pending'")
    int cancelPending(@Param("taskId") Long taskId, @Param("userId") Long userId);

    /**
     * 回收卡死的「下发中」记录
     *
     * <p>sending 是没有租约的占位态：进程若在占位之后中断（重启、被 kill），
     * 这条记录就再也没人处理，会永远停在 sending。调度每轮开始前把超时未更新的
     * sending 退回 pending，让它重新参与扫描。</p>
     *
     * @param deadline 早于该时刻更新的 sending 视为卡死
     * */
    @Update("update t_todo_task_reminder set status = 'pending', update_time = now() "
            + "where status = 'sending' and update_time < #{deadline}")
    int resetStaleSending(@Param("deadline") LocalDateTime deadline);
}
