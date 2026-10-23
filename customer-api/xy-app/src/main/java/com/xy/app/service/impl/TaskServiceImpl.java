package com.xy.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.app.mapper.TagMapper;
import com.xy.app.mapper.TaskMapper;
import com.xy.app.pojo.dto.TodoTaskArchiveDTO;
import com.xy.app.pojo.dto.TodoTaskCreateDTO;
import com.xy.app.pojo.dto.TodoTaskQueryDTO;
import com.xy.app.pojo.dto.TodoTaskStatusDTO;
import com.xy.app.pojo.dto.TodoTaskUpdateDTO;
import com.xy.app.pojo.vo.TodoMonthVO;
import com.xy.app.pojo.vo.TodoQuadrantVO;
import com.xy.app.pojo.vo.TodoTagStatVO;
import com.xy.app.pojo.vo.TodoTaskCountVO;
import com.xy.app.pojo.vo.TodoTaskCreateVO;
import com.xy.app.pojo.vo.TodoTaskDetailVO;
import com.xy.app.pojo.vo.TodoTaskPageVO;
import com.xy.app.pojo.vo.TodoTaskStatsVO;
import com.xy.app.pojo.vo.TodoWeekStatsVO;
import com.xy.app.security.TodoUserDetails;
import com.xy.app.service.ITaskService;
import com.xy.app.service.support.TodoTaskConverter;
import com.xy.app.service.support.TodoTaskValidator;
import com.xy.common.entity.todo.TodoTag;
import com.xy.common.entity.todo.TodoTask;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 客户端待办任务服务实现
 * */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, TodoTask> implements ITaskService {

    /** 列表状态筛选：待完成 */
    private static final String STATUS_PENDING = "pending";

    /** 列表状态筛选：已完成 */
    private static final String STATUS_DONE = "done";

    /** 列表默认页码 */
    private static final long DEFAULT_PAGE_NUM = 1L;

    /** 列表默认每页条数 */
    private static final long DEFAULT_PAGE_SIZE = 20L;

    /** 列表每页条数上限 */
    private static final long MAX_PAGE_SIZE = 100L;

    private final TagMapper tagMapper;

    private final TodoTaskSupport taskSupport;

    private final TodoRepeatSupport repeatSupport;

    private final TodoStatsSupport statsSupport;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TodoTaskCreateVO createTask(TodoTaskCreateDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();

        String title = TodoTaskValidator.validateTitle(dto.getTitle());
        TodoTaskValidator.validateDescription(dto.getDescription());
        int urgencyLevel = TodoTaskValidator.validateUrgencyLevel(dto.getUrgencyLevel());
        int priority = TodoTaskValidator.validatePriority(dto.getPriority());
        int remindBefore = TodoTaskValidator.validateRemindBefore(dto.getRemindBefore(), dto.getDueDate());

        // 幂等：同一个 clientId 重复提交，直接返回已建好的任务
        if (StringUtils.hasText(dto.getClientId())) {
            TodoTask exist = getByIdempotentKey(uId, dto.getClientId());
            if (exist != null) {
                return TodoTaskConverter.toCreateVO(exist);
            }
        }

        // 清单必须是当前用户自己的，避免越权挂到别人的清单上
        validateTagOwned(uId, dto.getTagId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remindAt = remindBefore > 0 ? dto.getDueDate().minusMinutes(remindBefore) : null;

        TodoTask task = new TodoTask();
        task.setUserId(uId);
        task.setTaskTitle(title);
        task.setDescription(dto.getDescription());
        task.setTagId(dto.getTagId());
        task.setPriority(priority);
        task.setUrgencyLevel(urgencyLevel);
        task.setDueDate(dto.getDueDate());
        task.setRepeatRule(dto.getRepeatRule() == null ? "" : dto.getRepeatRule());
        task.setRemindBefore(remindBefore);
        task.setRemindAt(remindAt);
        task.setClientId(dto.getClientId());
        task.setCompleted(0L);
        task.setArchived(0);
        task.setDeleted(0);
        task.setSeq(taskSupport.nextSeq(uId));
        task.setCreateBy(String.valueOf(uId));
        task.setUpdateBy(String.valueOf(uId));
        task.setCreateTime(now);
        task.setUpdateTime(now);

        try {
            if (!save(task)) {
                throw new BusinessException(ResultCode.OPERATION_FAILED, "创建任务失败");
            }
        } catch (DuplicateKeyException e) {
            // 并发重试撞上唯一键 uk_user_client，回查已建好的那条
            log.warn("并发创建任务命中幂等键，回查已有任务 uId={} clientId={}", uId, dto.getClientId());
            TodoTask exist = getByIdempotentKey(uId, dto.getClientId());
            if (exist == null) {
                throw e;
            }
            return TodoTaskConverter.toCreateVO(exist);
        }

        if (remindAt != null) {
            taskSupport.insertPendingReminder(task.getTaskId(), uId, remindAt, now);
        }

        return TodoTaskConverter.toCreateVO(task);
    }

    /**
     * 按用户 + 客户端幂等 Id 回查任务
     * */
    private TodoTask getByIdempotentKey(Long uId, String clientId) {
        return getOne(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, uId)
                .eq(TodoTask::getClientId, clientId), false);
    }

    @Override
    public TodoTaskPageVO listTasks(TodoTaskQueryDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();
        Long tagId = dto == null ? null : dto.getTagId();
        String status = dto == null || dto.getStatus() == null ? "" : dto.getStatus().trim().toLowerCase();
        // 搜索：关键词为空就当成不搜索，避免 like '%%' 白扫一遍
        String keyword = dto == null || !StringUtils.hasText(dto.getKeyword()) ? null : dto.getKeyword().trim();
        // 到期日筛选：只按「一天」过滤，取当天 00:00:00 ~ 次日 00:00:00 左闭右开区间，避免 23:59:59 的毫秒边界坑
        LocalDateTime dayStart = null;
        LocalDateTime dayEnd = null;

        if (dto != null && StringUtils.hasText(dto.getDate())) {
            try {
                dayStart = LocalDate.parse(dto.getDate().trim()).atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "日期格式不正确，应为 yyyy-MM-dd");
            }

            dayEnd = dayStart.plusDays(1);
        }

        boolean pendingOnly = STATUS_PENDING.equals(status);
        boolean doneOnly = STATUS_DONE.equals(status);
        // 象限筛选：传了才拼进 SQL，不传表示跨象限
        Integer urgencyLevel = dto == null ? null : dto.getUrgencyLevel();
        // 归档筛选：只有显式传 1 才查已归档，其余（不传、传 0、传别的）都按未归档处理
        int archived = dto != null && dto.getArchived() != null && dto.getArchived() == 1 ? 1 : 0;
        long pageNum = TodoTaskValidator.normalizePositive(dto == null ? null : dto.getPageNum(), DEFAULT_PAGE_NUM);
        long pageSize = Math.min(TodoTaskValidator.normalizePositive(dto == null ? null : dto.getPageSize(),
                DEFAULT_PAGE_SIZE), MAX_PAGE_SIZE);

        // 「全部/待完成/已完成」三个计数：分页后前端算不出来，这里一次 group by 拿全
        long pendingCount = 0L;
        long doneCount = 0L;

        for (TodoTaskCountVO count : baseMapper.countGroupByCompleted(uId, tagId, keyword, dayStart, dayEnd,
                urgencyLevel, archived)) {
            long countOfStatus = count.getTotal() == null ? 0L : count.getTotal();

            if (count.getCompleted() != null && count.getCompleted() == 1) {
                doneCount += countOfStatus;
            } else {
                pendingCount += countOfStatus;
            }
        }

        long total = pendingOnly ? pendingCount : doneOnly ? doneCount : pendingCount + doneCount;

        LambdaQueryWrapper<TodoTask> wrapper = new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getUserId, uId)
                .eq(TodoTask::getDeleted, 0)
                .eq(TodoTask::getArchived, archived)
                .eq(tagId != null, TodoTask::getTagId, tagId)
                .eq(urgencyLevel != null, TodoTask::getUrgencyLevel, urgencyLevel)
                .eq(pendingOnly, TodoTask::getCompleted, 0L)
                .eq(doneOnly, TodoTask::getCompleted, 1L)
                // 搜索：标题或备注命中都算，关键词为空时整段不拼进 SQL
                .and(keyword != null, w -> w.like(TodoTask::getTaskTitle, keyword)
                        .or()
                        .like(TodoTask::getDescription, keyword))
                // 到期日筛选：左闭右开区间，dayStart 为空时两条都不拼
                .ge(dayStart != null, TodoTask::getDueDate, dayStart)
                .lt(dayEnd != null, TodoTask::getDueDate, dayEnd);

        // 归档列表按「最近归档的在前」；主线列表保持未完成在前、快到期在前（没设日期的排最后），同组按创建时间倒序
        wrapper.last(archived == 1
                ? "order by archive_time is null asc, archive_time desc, completion_time desc, create_time desc"
                : "order by completed asc, due_date is null asc, due_date asc, create_time desc");

        // 计数上面已经拿全，这里跳过 count 查询（false），免得 order by 影响 count 优化
        Page<TodoTask> page = new Page<>(pageNum, pageSize, false);

        page(page, wrapper);

        TodoTaskPageVO vo = new TodoTaskPageVO();
        vo.setList(page.getRecords().stream().map(TodoTaskConverter::toListVO).toList());
        vo.setTotal(total);
        vo.setPageNum(pageNum);
        vo.setPageSize(pageSize);
        vo.setHasMore(pageNum * pageSize < total);
        vo.setAllCount(pendingCount + doneCount);
        vo.setPendingCount(pendingCount);
        vo.setDoneCount(doneCount);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(TodoTaskStatusDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();

        if (dto.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务Id不能为空");
        }

        Integer completed = dto.getCompleted();
        if (completed == null || (completed != 0 && completed != 1)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "完成状态只能是 0 或 1");
        }

        // 只能改自己的任务，避免拿着别人的 taskId 越权改状态
        TodoTask exist = getOwnedTask(uId, dto.getTaskId());

        if (exist == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务不存在");
        }

        // 已经是目标状态，直接返回（连点、重放都不会出错）
        if (exist.getCompleted() != null && exist.getCompleted().intValue() == completed.intValue()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // updateById 默认跳过 null 字段，取消完成时清不掉 completion_time，这里显式 set
        update(new LambdaUpdateWrapper<TodoTask>()
                .eq(TodoTask::getTaskId, dto.getTaskId())
                .eq(TodoTask::getUserId, uId)
                .set(TodoTask::getCompleted, completed.longValue())
                .set(TodoTask::getCompletionTime, completed == 1 ? now : null)
                .set(TodoTask::getUpdateBy, String.valueOf(uId))
                .set(TodoTask::getUpdateTime, now));

        // 重复任务：完成一次就滚动出下一次；点错了取消完成，再把这一次生成的收回去
        if (completed == 1) {
            repeatSupport.generateNextRepeat(exist, now);
        } else {
            repeatSupport.rollbackNextRepeat(dto.getTaskId(), uId, now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void archiveTask(TodoTaskArchiveDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();

        if (dto.getTaskId() == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务Id不能为空");
        }

        Integer archived = dto.getArchived();
        if (archived == null || (archived != 0 && archived != 1)) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "归档状态只能是 0 或 1");
        }

        // 只能改自己的任务，避免拿着别人的 taskId 越权归档
        TodoTask exist = getOwnedTask(uId, dto.getTaskId());

        if (exist == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务不存在");
        }

        // 归档是「做完之后收起来」：只允许归档已完成的任务，
        // 否则未完成的任务会被移出主线列表，它的提醒却还在，用户会收到一条看不到源头的提醒
        if (archived == 1 && (exist.getCompleted() == null || exist.getCompleted() != 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "只有已完成的任务才能归档");
        }

        // 已经是目标状态，直接返回（连点、重放都不会出错）
        if (exist.getArchived() != null && exist.getArchived() == archived) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 取消归档要能把 archive_time 清空，所以显式 set 而不是 updateById
        update(new LambdaUpdateWrapper<TodoTask>()
                .eq(TodoTask::getTaskId, dto.getTaskId())
                .eq(TodoTask::getUserId, uId)
                .set(TodoTask::getArchived, archived)
                .set(TodoTask::getArchiveTime, archived == 1 ? now : null)
                .set(TodoTask::getUpdateBy, String.valueOf(uId))
                .set(TodoTask::getUpdateTime, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int clearDoneTasks() {

        Long uId = TodoUserDetails.getCurrentUId();

        // 逻辑删除即可：两端列表与清单统计都带 deleted = 0，删掉就查不出来了
        return baseMapper.clearCompleted(uId);
    }

    @Override
    public TodoTaskStatsVO getTaskStats() {
        return statsSupport.getTaskStats();
    }

    @Override
    public TodoWeekStatsVO getWeekStats(Integer weekStart) {
        return statsSupport.getWeekStats(weekStart);
    }

    @Override
    public List<TodoTagStatVO> getTagDistribution() {
        return statsSupport.getTagDistribution();
    }

    @Override
    public TodoMonthVO getMonthStats(String month) {
        return statsSupport.getMonthStats(month);
    }

    @Override
    public TodoQuadrantVO getQuadrantGroups() {
        return statsSupport.getQuadrantGroups();
    }

    @Override
    public TodoTaskDetailVO getTaskDetail(Long taskId) {

        Long uId = TodoUserDetails.getCurrentUId();

        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务Id不能为空");
        }

        TodoTask task = getOwnedTask(uId, taskId);

        if (task == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务不存在");
        }

        TodoTaskDetailVO vo = TodoTaskConverter.toDetailVO(task);
        vo.setTagName(resolveTagName(task.getTagId()));

        return vo;
    }

    /**
     * 清单名：未归类、清单已删或查不到时统一显示「未归类」，与列表页口径一致
     * */
    private String resolveTagName(Long tagId) {
        if (tagId == null) {
            return "未归类";
        }

        TodoTag tag = tagMapper.selectById(tagId);

        return tag == null || !StringUtils.hasText(tag.getTagName()) ? "未归类" : tag.getTagName();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTask(Long taskId, TodoTaskUpdateDTO dto) {

        Long uId = TodoUserDetails.getCurrentUId();

        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务Id不能为空");
        }

        // 任务必须是自己的（顺带挡住改已删除的任务）
        requireOwnedTask(uId, taskId);

        String title = TodoTaskValidator.validateTitle(dto.getTitle());
        TodoTaskValidator.validateDescription(dto.getDescription());
        int urgencyLevel = TodoTaskValidator.validateUrgencyLevel(dto.getUrgencyLevel());
        int priority = TodoTaskValidator.validatePriority(dto.getPriority());
        int remindBefore = TodoTaskValidator.validateRemindBefore(dto.getRemindBefore(), dto.getDueDate());
        validateTagOwned(uId, dto.getTagId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime remindAt = remindBefore > 0 ? dto.getDueDate().minusMinutes(remindBefore) : null;

        // 全量覆盖，set 不跳过 null，所以清空备注、取消归类、去掉日期都能生效
        update(new LambdaUpdateWrapper<TodoTask>()
                .eq(TodoTask::getTaskId, taskId)
                .eq(TodoTask::getUserId, uId)
                .set(TodoTask::getTaskTitle, title)
                .set(TodoTask::getDescription, dto.getDescription())
                .set(TodoTask::getTagId, dto.getTagId())
                .set(TodoTask::getPriority, priority)
                .set(TodoTask::getUrgencyLevel, urgencyLevel)
                .set(TodoTask::getDueDate, dto.getDueDate())
                .set(TodoTask::getRepeatRule, dto.getRepeatRule() == null ? "" : dto.getRepeatRule())
                .set(TodoTask::getRemindBefore, remindBefore)
                .set(TodoTask::getRemindAt, remindAt)
                .set(TodoTask::getUpdateBy, String.valueOf(uId))
                .set(TodoTask::getUpdateTime, now));

        // 提醒点的时间必须跟着任务走：旧提醒点作废，再按新的 remindAt 重建一条，
        // 否则调度侧拿着旧时间会推错。至于「要不要发」由调度侧按任务状态判断，这里不管。
        taskSupport.cancelPendingReminder(taskId, uId);

        if (remindAt != null) {
            taskSupport.insertPendingReminder(taskId, uId, remindAt, now);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTask(Long taskId) {

        Long uId = TodoUserDetails.getCurrentUId();

        if (taskId == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务Id不能为空");
        }

        requireOwnedTask(uId, taskId);

        // 逻辑删除：清单维度统计、两端的列表查询都带 deleted = 0，删掉就查不出来了
        update(new LambdaUpdateWrapper<TodoTask>()
                .eq(TodoTask::getTaskId, taskId)
                .eq(TodoTask::getUserId, uId)
                .set(TodoTask::getDeleted, 1)
                .set(TodoTask::getUpdateBy, String.valueOf(uId))
                .set(TodoTask::getUpdateTime, LocalDateTime.now()));
    }

    /**
     * 查自己的任务（不含已删除），查不到返回 null
     * */
    private TodoTask getOwnedTask(Long uId, Long taskId) {
        return getOne(new LambdaQueryWrapper<TodoTask>()
                .eq(TodoTask::getTaskId, taskId)
                .eq(TodoTask::getUserId, uId)
                .eq(TodoTask::getDeleted, 0), false);
    }

    /**
     * 查自己的任务，查不到直接抛业务异常
     * */
    private void requireOwnedTask(Long uId, Long taskId) {
        if (getOwnedTask(uId, taskId) == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "任务不存在");
        }
    }

    /**
     * 校验清单归属：清单必须是自己的，避免把任务挂到别人的清单上
     * */
    private void validateTagOwned(Long uId, Long tagId) {
        if (tagId == null) {
            return;
        }

        TodoTag tag = tagMapper.selectById(tagId);
        if (tag == null || !uId.equals(tag.getUserId())) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "清单不存在");
        }
    }
}
