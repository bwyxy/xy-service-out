package com.xy.app.service.impl;

import com.xy.app.mapper.TaskReminderMapper;
import com.xy.app.mapper.UserSeqMapper;
import com.xy.app.service.INotifyService;
import com.xy.common.entity.todo.TodoTaskReminder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 待办任务落库辅助：提醒点写入/作废、用户同步序号发号
 * */
@Component
@RequiredArgsConstructor
public class TodoTaskSupport {

    /** 提醒状态：待下发 */
    private static final String REMIND_STATUS_PENDING = "pending";

    private final TaskReminderMapper taskReminderMapper;

    private final UserSeqMapper userSeqMapper;

    private final INotifyService notifyService;

    /**
     * 建一条待下发的提醒点
     * */
    public void insertPendingReminder(Long taskId, Long uId, LocalDateTime remindAt, LocalDateTime now) {

        TodoTaskReminder reminder = new TodoTaskReminder();
        reminder.setTaskId(taskId);
        reminder.setUserId(uId);
        reminder.setRemindAt(remindAt);
        reminder.setTemplateId(notifyService.getReminderTemplateId());
        reminder.setStatus(REMIND_STATUS_PENDING);
        reminder.setRetryCount(0);
        reminder.setFailReason("");
        reminder.setCreateTime(now);
        reminder.setUpdateTime(now);

        taskReminderMapper.insert(reminder);
    }

    /**
     * 作废某个任务下还没下发的提醒点（只改状态、保留记录）
     * */
    public void cancelPendingReminder(Long taskId, Long uId) {
        taskReminderMapper.cancelPending(taskId, uId);
    }

    /**
     * 取该用户下一个同步序号
     * */
    public Long nextSeq(Long uId) {
        userSeqMapper.increaseSeq(uId);

        Long seq = userSeqMapper.getSeq(uId);

        return seq == null ? 1L : seq;
    }
}
