package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.FocusSessionMapper;
import com.xy.app.pojo.dto.TodoFocusFinishDTO;
import com.xy.app.pojo.dto.TodoFocusStartDTO;
import com.xy.app.pojo.vo.TodoFocusTodayVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.IFocusService;
import com.xy.common.entity.todo.TodoFocusSession;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 客户端番茄专注服务实现
 * */
@Service
@RequiredArgsConstructor
public class FocusServiceImpl extends ServiceImpl<FocusSessionMapper, TodoFocusSession> implements IFocusService {

    /** 专注模式（只有这一种计入今日统计） */
    private static final String MODE_FOCUS = "focus";

    /** 短休息 */
    private static final String MODE_SHORT = "short";

    /** 长休息 */
    private static final String MODE_LONG = "long";

    /** 各模式的默认计划秒数，前端不传时按模式取 */
    private static final Map<String, Integer> DEFAULT_PLANNED_SECONDS = Map.of(
            MODE_FOCUS, 1500,
            MODE_SHORT, 300,
            MODE_LONG, 900
    );

    /** 进行中 */
    private static final String STATUS_RUNNING = "running";

    /** 完整跑完一轮 */
    private static final String STATUS_FINISHED = "finished";

    /** 中途放弃（暂停到离开页面、重置、切换模式） */
    private static final String STATUS_ABORTED = "aborted";

    /** 单轮计划秒数上限（8 小时），挡住明显不合理的上报 */
    private static final int MAX_PLANNED_SECONDS = 8 * 60 * 60;

    /** 单轮实际专注秒数上限，同上 */
    private static final int MAX_ACTUAL_SECONDS = 8 * 60 * 60;

    /** 结束宽限秒数：超过「计划秒数 + 宽限」还没结束的 running 记录，判定为进程被杀留下的 */
    private static final int STALE_GRACE_SECONDS = 600;

    /** clientId 长度上限（对齐 t_todo_focus_session.client_id varchar(64)） */
    private static final int CLIENT_ID_MAX_LENGTH = 64;

    @Override
    public TodoFocusTodayVO today() {
        Long uId = TodoUserDetails.getCurrentUId();

        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        // 先自愈：进程被杀、小程序被关时前端来不及上报结束，这类记录只会一直是 running，
        // 顺手收成 aborted（只收超过计划时长加宽限的，正在计时的那条不动）
        abortRunning(uId, LocalDateTime.now(), true);

        // 一次取回昨天与今天两天的完成记录，在内存里分开统计：一天最多几十条，不值当为它写聚合 SQL
        List<TodoFocusSession> rows = list(new LambdaQueryWrapper<TodoFocusSession>()
                .eq(TodoFocusSession::getUserId, uId)
                .eq(TodoFocusSession::getMode, MODE_FOCUS)
                .eq(TodoFocusSession::getStatus, STATUS_FINISHED)
                .ge(TodoFocusSession::getStartTime, todayStart.minusDays(1))
                .lt(TodoFocusSession::getStartTime, tomorrowStart));

        int todayCount = 0;
        int todaySeconds = 0;
        int yesterdayCount = 0;

        for (TodoFocusSession row : rows) {
            if (row.getStartTime() == null) {
                continue;
            }

            // 按开始时间归日：跨零点的一轮算在开始的那天
            if (row.getStartTime().isBefore(todayStart)) {
                yesterdayCount++;
            } else {
                todayCount++;
                todaySeconds += row.getActualSeconds() == null ? 0 : row.getActualSeconds();
            }
        }

        TodoFocusTodayVO vo = new TodoFocusTodayVO();
        vo.setTodayCount(todayCount);
        vo.setTodayMinutes(Math.round(todaySeconds / 60f));
        vo.setYesterdayCount(yesterdayCount);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(TodoFocusStartDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();
        String clientId = checkClientId(dto == null ? null : dto.getClientId());
        String mode = checkMode(dto.getMode());

        // 同一个 clientId 重复上报（弱网重试）只认第一条，直接当成功返回
        if (findSession(uId, clientId) != null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 上一轮还没结算就接着开始（进程被杀后重进页面），先把它收掉，避免同时存在两条进行中
        abortRunning(uId, now, false);

        TodoFocusSession session = new TodoFocusSession();
        session.setUserId(uId);
        session.setTaskId(dto.getTaskId());
        session.setMode(mode);
        session.setPlannedSeconds(checkPlannedSeconds(dto.getPlannedSeconds(), mode));
        session.setActualSeconds(0);
        session.setStartTime(now);
        session.setStatus(STATUS_RUNNING);
        session.setClientId(clientId);
        session.setCreateTime(now);

        save(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(TodoFocusFinishDTO dto) {
        Long uId = TodoUserDetails.getCurrentUId();
        String clientId = checkClientId(dto == null ? null : dto.getClientId());

        // 只认 finished，其余（含不传、乱传）一律按中途放弃处理
        String status = STATUS_FINISHED.equals(dto.getStatus()) ? STATUS_FINISHED : STATUS_ABORTED;
        LocalDateTime now = LocalDateTime.now();
        int actualSeconds = checkActualSeconds(dto.getActualSeconds());

        TodoFocusSession exist = findSession(uId, clientId);

        if (exist == null) {
            // start 的请求丢了（弱网下只到了一条 finish）：按 finish 带上来的信息补一条，别让这一轮白记
            String mode = checkMode(dto.getMode());

            TodoFocusSession session = new TodoFocusSession();
            session.setUserId(uId);
            session.setMode(mode);
            session.setPlannedSeconds(checkPlannedSeconds(dto.getPlannedSeconds(), mode));
            session.setActualSeconds(actualSeconds);
            // 开始时间由实际专注秒数倒推，统计归日才不会落到今天
            session.setStartTime(now.minusSeconds(actualSeconds));
            session.setEndTime(now);
            session.setStatus(status);
            session.setClientId(clientId);
            session.setCreateTime(now);

            save(session);

            return;
        }

        // 已经结算过的（重试、连点）不再改，保证幂等
        if (!STATUS_RUNNING.equals(exist.getStatus())) {
            return;
        }

        update(new LambdaUpdateWrapper<TodoFocusSession>()
                .eq(TodoFocusSession::getSessionId, exist.getSessionId())
                .eq(TodoFocusSession::getUserId, uId)
                .eq(TodoFocusSession::getStatus, STATUS_RUNNING)
                .set(TodoFocusSession::getStatus, status)
                .set(TodoFocusSession::getActualSeconds, actualSeconds)
                .set(TodoFocusSession::getEndTime, now));
    }

    /**
     * 把该用户遗留的进行中记录收成 aborted
     *
     * @param onlyStale true-只收超过「计划秒数 + 宽限」的（进页面统计时用，别打断正在计时的这一条）；
     *                  false-全部收掉（开始新一轮时用）
     * */
    private void abortRunning(Long uId, LocalDateTime now, boolean onlyStale) {
        List<TodoFocusSession> running = list(new LambdaQueryWrapper<TodoFocusSession>()
                .eq(TodoFocusSession::getUserId, uId)
                .eq(TodoFocusSession::getStatus, STATUS_RUNNING));

        for (TodoFocusSession session : running) {
            if (onlyStale && !isStale(session, now)) {
                continue;
            }

            // 条件里再带上 status：并发下只让一个请求把这条改掉
            update(new LambdaUpdateWrapper<TodoFocusSession>()
                    .eq(TodoFocusSession::getSessionId, session.getSessionId())
                    .eq(TodoFocusSession::getUserId, uId)
                    .eq(TodoFocusSession::getStatus, STATUS_RUNNING)
                    .set(TodoFocusSession::getStatus, STATUS_ABORTED)
                    .set(TodoFocusSession::getActualSeconds, elapsedSeconds(session, now))
                    .set(TodoFocusSession::getEndTime, now));
        }
    }

    /** 是否已超过「计划秒数 + 宽限」还没结束 */
    private boolean isStale(TodoFocusSession session, LocalDateTime now) {
        if (session.getStartTime() == null) {
            return true;
        }

        int planned = plannedOrDefault(session.getPlannedSeconds(), session.getMode());

        return !now.isBefore(session.getStartTime().plusSeconds(planned).plusSeconds(STALE_GRACE_SECONDS));
    }

    /** 估算的专注秒数：从开始到现在，最多不超过计划秒数；只给收尾的记录用，不计入统计 */
    private int elapsedSeconds(TodoFocusSession session, LocalDateTime now) {
        if (session.getStartTime() == null) {
            return 0;
        }

        long seconds = Duration.between(session.getStartTime(), now).getSeconds();

        if (seconds <= 0) {
            return 0;
        }

        return (int) Math.min(seconds, plannedOrDefault(session.getPlannedSeconds(), session.getMode()));
    }

    /** 取当前用户某个 clientId 的记录，没有返回 null */
    private TodoFocusSession findSession(Long uId, String clientId) {
        return getOne(new LambdaQueryWrapper<TodoFocusSession>()
                .eq(TodoFocusSession::getUserId, uId)
                .eq(TodoFocusSession::getClientId, clientId), false);
    }

    /**
     * 校验并规范化 clientId（必传，长度对齐表字段）
     * */
    private String checkClientId(String rawClientId) {
        if (!StringUtils.hasText(rawClientId)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "clientId 不能为空");
        }

        String clientId = rawClientId.trim();

        if (clientId.length() > CLIENT_ID_MAX_LENGTH) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "clientId 过长");
        }

        return clientId;
    }

    /**
     * 校验模式，不传按专注算
     * */
    private String checkMode(String rawMode) {
        if (rawMode == null || rawMode.isBlank()) {
            return MODE_FOCUS;
        }

        if (!DEFAULT_PLANNED_SECONDS.containsKey(rawMode)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "模式只能是 focus、short 或 long");
        }

        return rawMode;
    }

    /**
     * 校验并规范化计划秒数，不传按模式默认值
     * */
    private int checkPlannedSeconds(Integer plannedSeconds, String mode) {
        if (plannedSeconds == null || plannedSeconds <= 0) {
            return plannedOrDefault(null, mode);
        }

        return Math.min(plannedSeconds, MAX_PLANNED_SECONDS);
    }

    /**
     * 校验并规范化实际专注秒数，负数按 0 算
     * */
    private int checkActualSeconds(Integer actualSeconds) {
        if (actualSeconds == null || actualSeconds <= 0) {
            return 0;
        }

        return Math.min(actualSeconds, MAX_ACTUAL_SECONDS);
    }

    /**
     * 计划秒数兜底到模式默认值
     * */
    private int plannedOrDefault(Integer plannedSeconds, String mode) {
        if (plannedSeconds != null && plannedSeconds > 0) {
            return plannedSeconds;
        }

        return DEFAULT_PLANNED_SECONDS.getOrDefault(mode == null ? MODE_FOCUS : mode,
                DEFAULT_PLANNED_SECONDS.get(MODE_FOCUS));
    }
}
