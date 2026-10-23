package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xy.app.mapper.TaskMapper;
import com.xy.common.entity.todo.TodoTask;
import com.xy.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 重复任务滚动：完成一次生成下一条，取消完成把这一次生成的收回去
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoRepeatSupport {

    /** 重复规则：每天 */
    private static final String REPEAT_DAILY = "DAILY";

    /** 重复规则：每周 */
    private static final String REPEAT_WEEKLY = "WEEKLY";

    /** 重复规则：每月 */
    private static final String REPEAT_MONTHLY = "MONTHLY";

    /** 往后找下一次时最多尝试多少个周期（按每天算约 100 年），防规则异常时死循环 */
    private static final int MAX_REPEAT_STEPS = 40000;

    private final TaskMapper taskMapper;

    private final TodoTaskSupport taskSupport;

    /**
     * 重复任务完成一次后，按规则复制出下一条
     * */
    public void generateNextRepeat(TodoTask task, LocalDateTime now) {

        LocalDateTime nextDueDate = nextDueDate(task.getRepeatRule(), task.getDueDate(), now);

        if (nextDueDate == null) {
            return;
        }

        Long uId = task.getUserId();
        int remindBefore = task.getRemindBefore() == null ? 0 : task.getRemindBefore();
        LocalDateTime remindAt = remindBefore > 0 ? nextDueDate.minusMinutes(remindBefore) : null;

        TodoTask next = new TodoTask();
        next.setUserId(uId);
        next.setTaskTitle(task.getTaskTitle());
        next.setDescription(task.getDescription());
        next.setTagId(task.getTagId());
        next.setPriority(task.getPriority());
        next.setUrgencyLevel(task.getUrgencyLevel());
        next.setDueDate(nextDueDate);
        next.setRepeatRule(task.getRepeatRule());
        next.setRemindBefore(remindBefore);
        next.setRemindAt(remindAt);
        next.setRepeatSourceId(task.getTaskId());
        // 幂等键不能继承：它标识的是「原任务那一次提交」，复制过来会让新任务被当成重复提交
        next.setClientId(null);
        next.setCompleted(0L);
        next.setArchived(0);
        next.setDeleted(0);
        next.setSeq(taskSupport.nextSeq(uId));
        next.setCreateBy(String.valueOf(uId));
        next.setUpdateBy(String.valueOf(uId));
        next.setCreateTime(now);
        next.setUpdateTime(now);

        taskMapper.insert(next);

        if (remindAt != null) {
            taskSupport.insertPendingReminder(next.getTaskId(), uId, remindAt, now);
        }

        log.info("重复任务滚动出下一条 sourceTaskId={} nextTaskId={} nextDueDate={}",
                task.getTaskId(), next.getTaskId(), nextDueDate);
    }

    /**
     * 取消完成时，把这一次完成生成的下一期收回去
     *
     * <p>只收回还没被完成的那条：如果它已经被完成、又生成了再下一条，删它就把链断掉了。</p>
     * */
    public void rollbackNextRepeat(Long taskId, Long uId, LocalDateTime now) {

        List<TodoTask> generated = taskMapper.selectList(new LambdaQueryWrapper<TodoTask>()
                .select(TodoTask::getTaskId)
                .eq(TodoTask::getUserId, uId)
                .eq(TodoTask::getRepeatSourceId, taskId)
                .eq(TodoTask::getCompleted, 0L));

        for (TodoTask item : generated) {
            taskMapper.update(null, new LambdaUpdateWrapper<TodoTask>()
                    .eq(TodoTask::getTaskId, item.getTaskId())
                    .eq(TodoTask::getUserId, uId)
                    .set(TodoTask::getDeleted, 1)
                    .set(TodoTask::getUpdateBy, String.valueOf(uId))
                    .set(TodoTask::getUpdateTime, now));

            taskSupport.cancelPendingReminder(item.getTaskId(), uId);

            log.info("取消完成，收回重复任务下一条 taskId={} sourceTaskId={}", item.getTaskId(), taskId);
        }
    }

    /**
     * 算下一次到期时间
     *
     * <p>从原到期时间起按周期往后找，取第一个晚于当前时间的结果：逾期很久的每日任务
     * 不该生成一条「已经过期」的下一次。规则认不出来（空串、设计稿遗留格式）一律返回
     * null，当不重复处理，不猜。</p>
     * */
    private LocalDateTime nextDueDate(String repeatRule, LocalDateTime dueDate, LocalDateTime now) {

        if (StringUtils.isBlank(repeatRule) || dueDate == null) {
            return null;
        }

        String rule = repeatRule.trim().toUpperCase();

        for (int periods = 1; periods <= MAX_REPEAT_STEPS; periods++) {
            LocalDateTime next = plusPeriods(dueDate, rule, periods);

            if (next == null) {
                return null;
            }

            if (next.isAfter(now)) {
                return next;
            }
        }

        return null;
    }

    /**
     * 从原到期时间往后推 n 个周期，认不出的规则返回 null
     *
     * <p>注意是「原到期时间 + n 个周期」而不是在上一次结果上再加一个周期：
     * 后者会让每月 31 日的任务在 2 月被夹到 28 日之后再也回不到 31 日。
     * 每月用 plusMonths 而不是加 30 天，月末由它自己夹到当月最后一天。</p>
     * */
    private LocalDateTime plusPeriods(LocalDateTime base, String rule, int periods) {
        return switch (rule) {
            case REPEAT_DAILY -> base.plusDays(periods);
            case REPEAT_WEEKLY -> base.plusWeeks(periods);
            case REPEAT_MONTHLY -> base.plusMonths(periods);
            default -> null;
        };
    }
}
