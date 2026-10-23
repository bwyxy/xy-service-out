package com.xy.app.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xy.app.config.WeChatSubscribeProperties;
import com.xy.app.mapper.TaskMapper;
import com.xy.app.mapper.TaskReminderMapper;
import com.xy.app.response.WeChatApiResponse;
import com.xy.app.service.INotifyService;
import com.xy.app.service.IUserSettingService;
import com.xy.common.entity.User;
import com.xy.common.entity.todo.TodoNotifyLog;
import com.xy.common.entity.todo.TodoTask;
import com.xy.common.entity.todo.TodoTaskReminder;
import com.xy.common.entity.todo.TodoUserSetting;
import com.xy.common.utils.StringUtils;
import com.xy.system.service.IChatUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 任务提醒下发调度器
 *
 * <p>一次性订阅「授权一次只能发一条」，所以每一轮下发前都要过四道闸：
 * 任务是否还有效、用户开关、免打扰时段、剩余额度。任何一道没过都不能发，
 * 否则要么骚扰用户，要么白白浪费一次授权。</p>
 *
 * <p>用 CAS 把 pending 改成 sending 来占位，而不是加 Redis 锁：改状态和判断在一条 SQL 里，
 * 影响行数为 0 说明已被别的线程/实例抢先，直接跳过，天然防重入。</p>
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoRemindScheduler {

    /** 待下发 */
    private static final String STATUS_PENDING = "pending";

    /** 下发中（占位） */
    private static final String STATUS_SENDING = "sending";

    /** 已下发 */
    private static final String STATUS_SENT = "sent";

    /** 下发失败（重试用尽） */
    private static final String STATUS_FAILED = "failed";

    /** 已作废 */
    private static final String STATUS_CANCELED = "canceled";

    /** HH:mm */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TaskReminderMapper taskReminderMapper;

    private final TaskMapper taskMapper;

    private final IChatUserService chatUserService;

    private final INotifyService notifyService;

    private final IUserSettingService userSettingService;

    private final WeChatSubscribeProperties subscribeProperties;

    /** 每轮最多处理多少条，避免一次扫太多把接口打爆 */
    @Value("${todo.remind.batch-size:200}")
    private int batchSize;

    /** 下发失败最多重试次数 */
    @Value("${todo.remind.max-retry:3}")
    private int maxRetry;

    /** 占位「下发中」超过多少分钟视为卡死，回收重发 */
    @Value("${todo.remind.sending-timeout-minutes:10}")
    private int sendingTimeoutMinutes;

    /**
     * 定时扫描到点的提醒并下发
     * */
    @Scheduled(fixedDelayString = "${todo.remind.scan-interval-ms:30000}")
    public void scanAndSend() {

        LocalDateTime now = LocalDateTime.now();

        // 先回收卡死的占位：sending 没有租约，进程中断后这些记录会永远停在 sending
        recycleStaleSending(now);

        List<TodoTaskReminder> reminders = taskReminderMapper.selectList(new LambdaQueryWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getStatus, STATUS_PENDING)
                .le(TodoTaskReminder::getRemindAt, now)
                .orderByAsc(TodoTaskReminder::getRemindAt)
                .last("limit " + batchSize));

        if (reminders.isEmpty()) {
            return;
        }

        for (TodoTaskReminder reminder : reminders) {
            try {
                handleOne(reminder);
            } catch (Exception e) {
                // 单条异常不能中断整轮，否则一条坏数据会把后面的提醒全部卡住
                log.error("处理提醒异常 reminderId={} err={}", reminder.getReminderId(), e.getMessage(), e);

                // 异常也要累加重试次数，否则持续异常（如微信凭证拿不到）会每轮无限重试
                String failReason = "处理异常: " + e.getMessage();
                int retryCount = (reminder.getRetryCount() == null ? 0 : reminder.getRetryCount()) + 1;

                if (retryCount < maxRetry) {
                    markRetry(reminder.getReminderId(), retryCount, failReason, now);
                } else {
                    markFailed(reminder.getReminderId(), retryCount, failReason, now);
                }
            }
        }
    }

    /**
     * 把超时未更新的「下发中」退回待下发
     *
     * <p>宁可极小概率重复发一条，也不能让提醒永久卡死：卡死的记录既不会再发出去，
     * 状态也对不上，用户看到的就是「收到了但状态没变」的反面 —— 收不到且状态也不变。</p>
     * */
    private void recycleStaleSending(LocalDateTime now) {

        int recycled = taskReminderMapper.resetStaleSending(now.minusMinutes(sendingTimeoutMinutes));

        if (recycled > 0) {
            log.warn("回收卡死的下发中提醒 {} 条（超过 {} 分钟未更新）", recycled, sendingTimeoutMinutes);
        }
    }

    /**
     * 处理一条到点的提醒
     * */
    private void handleOne(TodoTaskReminder reminder) {

        LocalDateTime now = LocalDateTime.now();

        // 1. CAS 占位：抢不到说明别的线程正在处理 / 已处理过
        if (!claim(reminder.getReminderId(), now)) {
            return;
        }

        Long taskId = reminder.getTaskId();
        Long uId = reminder.getUserId();

        // 2. 任务是否还有效：已删除 / 已完成的任务不该再提醒
        TodoTask task = taskMapper.selectById(taskId);

        if (task == null || (task.getDeleted() != null && task.getDeleted() == 1)
                || (task.getCompleted() != null && task.getCompleted() == 1)) {
            markCanceled(reminder.getReminderId(), "任务已删除或已完成", now);
            log.info("提醒作废（任务失效） reminderId={} taskId={}", reminder.getReminderId(), taskId);

            return;
        }

        // 3. 用户开关：关掉提醒就别发了，直接作废而不是留着
        TodoUserSetting setting = userSettingService.getOrDefault(uId);

        if (setting.getTaskRemindOn() != null && setting.getTaskRemindOn() == 0) {
            markCanceled(reminder.getReminderId(), "用户已关闭任务提醒", now);
            log.info("提醒作废（提醒开关关闭） reminderId={} uId={}", reminder.getReminderId(), uId);

            return;
        }

        // 4. 免打扰：顺延到免打扰结束，而不是丢弃（丢了用户就永远收不到）
        LocalDateTime quietEnd = resolveNextQuietEnd(setting, now);

        if (quietEnd != null) {
            releaseToPending(reminder, quietEnd, true);
            log.info("提醒顺延（免打扰时段） reminderId={} uId={} to={}", reminder.getReminderId(), uId, quietEnd);

            return;
        }

        // 5. 额度：扣不动就退回 pending，等用户再授权后自然重发。
        //    这里刻意不写日志，否则每轮扫描都会刷一条，日志表很快被废数据淹掉
        if (!notifyService.consumeReminderQuota(uId)) {
            releaseToPending(reminder, reminder.getRemindAt(), false);
            log.info("提醒暂缓（订阅额度不足） reminderId={} uId={}", reminder.getReminderId(), uId);

            return;
        }

        // 6. openid：拿到说明用户微信侧还在，拿不到只能作废
        User user = chatUserService.getByUId(uId);
        String openid = user == null ? null : user.getWxOpenId();

        if (StringUtils.isBlank(openid)) {
            markCanceled(reminder.getReminderId(), "用户 openid 不存在", now);
            writeLog(reminder, null, false, now);
            log.warn("提醒作废（无 openid） reminderId={} uId={}", reminder.getReminderId(), uId);

            return;
        }

        // 7. 发送
        WeChatApiResponse response = notifyService.sendTaskReminder(openid, task, reminder.getRemindAt());

        boolean success = response != null && response.getErrcode() != null && response.getErrcode() == 0;

        if (success) {
            markSent(reminder.getReminderId(), now);
        } else {
            int retryCount = (reminder.getRetryCount() == null ? 0 : reminder.getRetryCount()) + 1;
            String failReason = buildFailReason(response);

            if (retryCount < maxRetry) {
                markRetry(reminder.getReminderId(), retryCount, failReason, now);
            } else {
                markFailed(reminder.getReminderId(), retryCount, failReason, now);
            }

            log.warn("提醒下发失败 reminderId={} uId={} retry={} reason={}",
                    reminder.getReminderId(), uId, retryCount, failReason);
        }

        // 8. 无论成败都记日志：这是排查「用户说没收到提醒」的唯一凭据
        writeLog(reminder, response, success, now);
    }

    /**
     * CAS 抢占：只有把 pending 改成 sending 成功的那一方才能继续
     * */
    private boolean claim(Long reminderId, LocalDateTime now) {
        int rows = taskReminderMapper.update(null, new LambdaUpdateWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getReminderId, reminderId)
                .eq(TodoTaskReminder::getStatus, STATUS_PENDING)
                .set(TodoTaskReminder::getStatus, STATUS_SENDING)
                .set(TodoTaskReminder::getUpdateTime, now));

        return rows > 0;
    }

    /**
     * 退回待下发，可选改期
     *
     * @param changeRemindAt 是否把提醒时间改成 remindAt
     * */
    private void releaseToPending(TodoTaskReminder reminder, LocalDateTime remindAt, boolean changeRemindAt) {

        LambdaUpdateWrapper<TodoTaskReminder> wrapper = new LambdaUpdateWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getReminderId, reminder.getReminderId())
                .set(TodoTaskReminder::getStatus, STATUS_PENDING)
                .set(TodoTaskReminder::getUpdateTime, LocalDateTime.now());

        if (changeRemindAt) {
            wrapper.set(TodoTaskReminder::getRemindAt, remindAt);
        }

        taskReminderMapper.update(null, wrapper);
    }

    /**
     * 下发失败但还能重试：退回 pending 并累加重试次数
     * */
    private void markRetry(Long reminderId, int retryCount, String failReason, LocalDateTime now) {
        taskReminderMapper.update(null, new LambdaUpdateWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getReminderId, reminderId)
                .set(TodoTaskReminder::getStatus, STATUS_PENDING)
                .set(TodoTaskReminder::getRetryCount, retryCount)
                .set(TodoTaskReminder::getFailReason, failReason)
                .set(TodoTaskReminder::getUpdateTime, now));
    }

    /**
     * 下发成功
     * */
    private void markSent(Long reminderId, LocalDateTime now) {
        taskReminderMapper.update(null, new LambdaUpdateWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getReminderId, reminderId)
                .set(TodoTaskReminder::getStatus, STATUS_SENT)
                .set(TodoTaskReminder::getSendTime, now)
                .set(TodoTaskReminder::getFailReason, "")
                .set(TodoTaskReminder::getUpdateTime, now));
    }

    /**
     * 重试用尽，最终失败
     * */
    private void markFailed(Long reminderId, int retryCount, String failReason, LocalDateTime now) {
        taskReminderMapper.update(null, new LambdaUpdateWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getReminderId, reminderId)
                .set(TodoTaskReminder::getStatus, STATUS_FAILED)
                .set(TodoTaskReminder::getRetryCount, retryCount)
                .set(TodoTaskReminder::getFailReason, failReason)
                .set(TodoTaskReminder::getUpdateTime, now));
    }

    /**
     * 作废这条提醒
     * */
    private void markCanceled(Long reminderId, String reason, LocalDateTime now) {
        taskReminderMapper.update(null, new LambdaUpdateWrapper<TodoTaskReminder>()
                .eq(TodoTaskReminder::getReminderId, reminderId)
                .set(TodoTaskReminder::getStatus, STATUS_CANCELED)
                .set(TodoTaskReminder::getFailReason, reason)
                .set(TodoTaskReminder::getUpdateTime, now));
    }

    /**
     * 当前是否落在免打扰时段，落在里面就返回该顺延到的时刻，否则返回 null
     *
     * <p>起止相同视为未启用；跨零点（如 22:00~07:00）要分成两段判断。</p>
     * */
    private LocalDateTime resolveNextQuietEnd(TodoUserSetting setting, LocalDateTime now) {

        LocalTime start = parseTime(setting.getQuietStart());
        LocalTime end = parseTime(setting.getQuietEnd());

        if (start == null || end == null || start.equals(end)) {
            return null;
        }

        LocalTime current = now.toLocalTime();

        boolean inQuiet = start.isBefore(end)
                ? (!current.isBefore(start) && current.isBefore(end))
                : (!current.isBefore(start) || current.isBefore(end));

        if (!inQuiet) {
            return null;
        }

        LocalDateTime nextQuietEnd = LocalDateTime.of(LocalDate.from(now.toLocalDate()), end);

        return nextQuietEnd.isAfter(now) ? nextQuietEnd : nextQuietEnd.plusDays(1);
    }

    /**
     * 解析 HH:mm，非法值当作未配置（宁可发出去也别把提醒永久卡住）
     * */
    private LocalTime parseTime(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }

        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (Exception e) {
            log.warn("免打扰时间格式非法 value={}", value);

            return null;
        }
    }

    private String buildFailReason(WeChatApiResponse response) {
        if (response == null) {
            return "微信接口无响应";
        }

        String reason = response.getErrcode() + " " + response.getErrmsg();

        return StringUtils.isBlank(response.getErrmsg()) ? String.valueOf(response.getErrcode()) : reason;
    }

    /**
     * 落一条下发日志
     * */
    private void writeLog(TodoTaskReminder reminder, WeChatApiResponse response, boolean success, LocalDateTime now) {

        TodoNotifyLog notifyLog = new TodoNotifyLog();
        notifyLog.setUserId(reminder.getUserId());
        notifyLog.setTaskId(reminder.getTaskId());
        notifyLog.setReminderId(reminder.getReminderId());
        notifyLog.setTemplateId(reminder.getTemplateId());
        notifyLog.setPage(subscribeProperties.getPage());
        notifyLog.setSendTime(now);

        if (success) {
            notifyLog.setErrcode(0);
            notifyLog.setErrmsg("ok");
        } else if (response != null) {
            notifyLog.setErrcode(response.getErrcode());
            notifyLog.setErrmsg(response.getErrmsg());
        }

        notifyService.writeLog(notifyLog);
    }
}
