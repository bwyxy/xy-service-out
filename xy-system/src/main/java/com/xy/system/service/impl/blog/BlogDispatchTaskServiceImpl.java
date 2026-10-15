package com.xy.system.service.impl.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xy.common.core.domain.PageResult;
import com.xy.common.dto.blog.DispatchRequestDTO;
import com.xy.common.entity.SysArticle;
import com.xy.common.entity.blog.BlogDispatchTask;
import com.xy.common.enums.DispatchErrorTypeEnum;
import com.xy.common.enums.DispatchStatusEnum;
import com.xy.common.exception.ServiceException;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.common.vo.blog.DispatchSubmitVO;
import com.xy.common.vo.blog.DispatchTaskVO;
import com.xy.system.dispatch.DispatchTaskExecutor;
import com.xy.system.mapper.blog.BlogDispatchTaskMapper;
import com.xy.system.service.ISysArticleService;
import com.xy.system.service.blog.IBlogDispatchPlatformService;
import com.xy.system.service.blog.IBlogDispatchTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 分发任务服务实现
 * 提交阶段只做校验与任务落库，真实分发由 DispatchTaskExecutor 异步执行，不阻塞后台操作
 * */
@Slf4j
@Service
public class BlogDispatchTaskServiceImpl extends ServiceImpl<BlogDispatchTaskMapper, BlogDispatchTask>
        implements IBlogDispatchTaskService {

    private static final DateTimeFormatter BATCH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private static final int MAX_ERROR_MSG_LENGTH = 1000;

    private final IBlogDispatchPlatformService platformService;

    private final ISysArticleService articleService;

    private final ObjectProvider<DispatchTaskExecutor> executorProvider;

    public BlogDispatchTaskServiceImpl(IBlogDispatchPlatformService platformService,
                                       ISysArticleService articleService,
                                       ObjectProvider<DispatchTaskExecutor> executorProvider) {
        this.platformService = platformService;
        this.articleService = articleService;
        this.executorProvider = executorProvider;
    }

    @Override
    public DispatchSubmitVO submitDispatch(DispatchRequestDTO request) {
        if (request == null || request.getArticleIds() == null || request.getArticleIds().isEmpty()) {
            throw new ServiceException("请至少选择一篇待分发的文章");
        }
        if (request.getPlatformCodes() == null || request.getPlatformCodes().isEmpty()) {
            throw new ServiceException("请至少选择一个目标平台");
        }

        boolean ignoreDispatched = request.getIgnoreDispatched() == null || request.getIgnoreDispatched();
        DispatchSubmitVO submitVO = new DispatchSubmitVO();
        String batchNo = generateBatchNo();
        submitVO.setBatchNo(batchNo);

        Set<Long> articleIds = new LinkedHashSet<>(request.getArticleIds());
        Set<String> platformCodes = new LinkedHashSet<>(request.getPlatformCodes());
        List<BlogDispatchTask> tasks = new ArrayList<>();
        Long operatorId = currentUserId();

        for (String platformCode : platformCodes) {
            var platform = platformService.getByCode(platformCode);
            if (platform == null) {
                skip(submitVO, "平台【" + platformCode + "】尚未配置，已跳过");
                continue;
            }
            if (platform.getEnabled() == null || platform.getEnabled() != 1) {
                skip(submitVO, "平台【" + platform.getPlatformName() + "】未启用，已跳过");
                continue;
            }
            if (!platformService.canUsePlatform(platformCode)) {
                skip(submitVO, "当前账号无权使用平台【" + platform.getPlatformName() + "】，已跳过");
                continue;
            }

            for (Long articleId : articleIds) {
                SysArticle article = articleId == null ? null : articleService.selectSysArticleById(articleId);
                if (article == null) {
                    skip(submitVO, "文章[" + articleId + "]不存在，已跳过");
                    continue;
                }
                if (ignoreDispatched && hasSucceeded(articleId, platformCode)) {
                    skip(submitVO, "文章【" + article.getTitle() + "】已成功分发到平台【"
                            + platform.getPlatformName() + "】，已跳过");
                    continue;
                }
                BlogDispatchTask task = new BlogDispatchTask();
                task.setBatchNo(batchNo);
                task.setArticleId(articleId);
                task.setArticleTitle(truncate(article.getTitle(), 255));
                task.setPlatformCode(platformCode);
                task.setPlatformName(platform.getPlatformName());
                task.setStatus(DispatchStatusEnum.PENDING.getCode());
                task.setRetryCount(0);
                task.setMaxRetry(platform.getMaxRetry() == null ? 3 : platform.getMaxRetry());
                task.setOperatorId(operatorId);
                tasks.add(task);
            }
        }

        if (tasks.isEmpty()) {
            submitVO.setCreatedCount(0);
            submitVO.setSkippedCount(submitVO.getSkippedReasons().size());
            return submitVO;
        }

        saveBatch(tasks);
        submitVO.setCreatedCount(tasks.size());
        submitVO.setSkippedCount(submitVO.getSkippedReasons().size());
        for (BlogDispatchTask task : tasks) {
            submitVO.getTaskIds().add(task.getId());
        }

        for (BlogDispatchTask task : tasks) {
            executorProvider.getObject().execute(task.getId());
        }
        log.info("分发任务提交完成 batchNo={} 新建 {} 条，跳过 {} 条", batchNo, tasks.size(),
                submitVO.getSkippedCount());
        return submitVO;
    }

    @Override
    public PageResult<DispatchTaskVO> selectTaskPage(BlogDispatchTask query) {
        BlogDispatchTask condition = query == null ? new BlogDispatchTask() : query;
        LambdaQueryWrapper<BlogDispatchTask> wrapper = new LambdaQueryWrapper<>();
        if (!StringUtils.isEmpty(condition.getBatchNo())) {
            wrapper.eq(BlogDispatchTask::getBatchNo, condition.getBatchNo());
        }
        if (condition.getArticleId() != null) {
            wrapper.eq(BlogDispatchTask::getArticleId, condition.getArticleId());
        }
        if (!StringUtils.isEmpty(condition.getPlatformCode())) {
            wrapper.eq(BlogDispatchTask::getPlatformCode, condition.getPlatformCode());
        }
        if (condition.getStatus() != null) {
            wrapper.eq(BlogDispatchTask::getStatus, condition.getStatus());
        }
        if (!StringUtils.isEmpty(condition.getArticleTitle())) {
            wrapper.like(BlogDispatchTask::getArticleTitle, condition.getArticleTitle());
        }
        wrapper.orderByDesc(BlogDispatchTask::getId);

        Page<BlogDispatchTask> page = this.page(new Page<>(condition.getPageNum(), condition.getPageSize()), wrapper);
        List<DispatchTaskVO> list = new ArrayList<>(page.getRecords().size());
        for (BlogDispatchTask task : page.getRecords()) {
            list.add(toVO(task));
        }
        return PageResult.of(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public DispatchTaskVO selectTaskById(Long id) {
        if (id == null) {
            throw new ServiceException("任务Id 不能为空");
        }
        BlogDispatchTask task = getById(id);
        if (task == null) {
            throw new ServiceException("分发任务不存在");
        }
        return toVO(task);
    }

    @Override
    public void retryTasks(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new ServiceException("请至少选择一个待重试的任务");
        }
        for (Long taskId : new LinkedHashSet<>(taskIds)) {
            BlogDispatchTask task = taskId == null ? null : getById(taskId);
            if (task == null) {
                continue;
            }
            if (DispatchStatusEnum.RUNNING.getCode().equals(task.getStatus())) {
                continue;
            }
            LambdaUpdateWrapper<BlogDispatchTask> update = new LambdaUpdateWrapper<>();
            update.eq(BlogDispatchTask::getId, taskId)
                    .set(BlogDispatchTask::getStatus, DispatchStatusEnum.PENDING.getCode())
                    .set(BlogDispatchTask::getErrorType, null)
                    .set(BlogDispatchTask::getErrorMsg, null)
                    .set(BlogDispatchTask::getFinishTime, null);
            update(update);
            executorProvider.getObject().execute(taskId);
        }
    }

    @Override
    public void deleteTasks(List<Long> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) {
            throw new ServiceException("请至少选择一个待删除的任务");
        }
        LambdaQueryWrapper<BlogDispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(BlogDispatchTask::getId, new LinkedHashSet<>(taskIds))
                .ne(BlogDispatchTask::getStatus, DispatchStatusEnum.RUNNING.getCode());
        remove(wrapper);
    }

    @Override
    public List<Long> selectPendingTaskIds(int limit) {
        LambdaQueryWrapper<BlogDispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchTask::getStatus, DispatchStatusEnum.PENDING.getCode())
                .orderByAsc(BlogDispatchTask::getId)
                .last("limit " + Math.max(1, limit));
        List<BlogDispatchTask> tasks = this.list(wrapper);
        List<Long> ids = new ArrayList<>(tasks.size());
        for (BlogDispatchTask task : tasks) {
            ids.add(task.getId());
        }
        return ids;
    }

    @Override
    public List<Long> selectRetryableTaskIds(int limit) {
        LambdaQueryWrapper<BlogDispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchTask::getStatus, DispatchStatusEnum.FAILED.getCode())
                .and(w -> w.isNull(BlogDispatchTask::getRetryCount)
                        .or().apply("retry_count < max_retry"))
                .orderByAsc(BlogDispatchTask::getId)
                .last("limit " + Math.max(1, limit));
        List<BlogDispatchTask> tasks = this.list(wrapper);
        List<Long> ids = new ArrayList<>();
        for (BlogDispatchTask task : tasks) {
            DispatchErrorTypeEnum errorType = DispatchErrorTypeEnum.fromCode(task.getErrorType());
            if (errorType.isRetryable()) {
                ids.add(task.getId());
            }
        }
        return ids;
    }

    @Override
    public boolean markRunning(Long taskId) {
        LambdaUpdateWrapper<BlogDispatchTask> update = new LambdaUpdateWrapper<>();
        update.eq(BlogDispatchTask::getId, taskId)
                .in(BlogDispatchTask::getStatus, DispatchStatusEnum.PENDING.getCode(),
                        DispatchStatusEnum.FAILED.getCode())
                .set(BlogDispatchTask::getStatus, DispatchStatusEnum.RUNNING.getCode())
                .set(BlogDispatchTask::getStartTime, LocalDateTime.now())
                .set(BlogDispatchTask::getErrorType, null)
                .set(BlogDispatchTask::getErrorMsg, null);
        return update(update);
    }

    @Override
    public void markSuccess(Long taskId, String platformArticleId, String resultUrl, long durationMs) {
        LambdaUpdateWrapper<BlogDispatchTask> update = new LambdaUpdateWrapper<>();
        update.eq(BlogDispatchTask::getId, taskId)
                .set(BlogDispatchTask::getStatus, DispatchStatusEnum.SUCCESS.getCode())
                .set(BlogDispatchTask::getPlatformArticleId, platformArticleId)
                .set(BlogDispatchTask::getResultUrl, resultUrl)
                .set(BlogDispatchTask::getErrorType, DispatchErrorTypeEnum.NONE.getCode())
                .set(BlogDispatchTask::getErrorMsg, null)
                .set(BlogDispatchTask::getDurationMs, durationMs)
                .set(BlogDispatchTask::getFinishTime, LocalDateTime.now());
        update(update);
    }

    @Override
    public void markFailed(Long taskId, DispatchErrorTypeEnum errorType, String errorMsg, long durationMs) {
        DispatchErrorTypeEnum type = errorType == null ? DispatchErrorTypeEnum.UNKNOWN : errorType;
        LambdaUpdateWrapper<BlogDispatchTask> update = new LambdaUpdateWrapper<>();
        update.eq(BlogDispatchTask::getId, taskId)
                .set(BlogDispatchTask::getStatus, DispatchStatusEnum.FAILED.getCode())
                .set(BlogDispatchTask::getErrorType, type.getCode())
                .set(BlogDispatchTask::getErrorMsg, truncate(errorMsg, MAX_ERROR_MSG_LENGTH))
                .set(BlogDispatchTask::getDurationMs, durationMs)
                .set(BlogDispatchTask::getFinishTime, LocalDateTime.now())
                .setSql("retry_count = retry_count + 1");
        update(update);
    }

    private boolean hasSucceeded(Long articleId, String platformCode) {
        LambdaQueryWrapper<BlogDispatchTask> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogDispatchTask::getArticleId, articleId)
                .eq(BlogDispatchTask::getPlatformCode, platformCode)
                .eq(BlogDispatchTask::getStatus, DispatchStatusEnum.SUCCESS.getCode());
        return count(wrapper) > 0;
    }

    private DispatchTaskVO toVO(BlogDispatchTask task) {
        DispatchTaskVO vo = new DispatchTaskVO();
        vo.setId(task.getId());
        vo.setBatchNo(task.getBatchNo());
        vo.setArticleId(task.getArticleId());
        vo.setArticleTitle(task.getArticleTitle());
        vo.setPlatformCode(task.getPlatformCode());
        vo.setPlatformName(task.getPlatformName());
        vo.setStatus(task.getStatus());
        vo.setStatusDesc(DispatchStatusEnum.descOf(task.getStatus()));
        vo.setRetryCount(task.getRetryCount());
        vo.setMaxRetry(task.getMaxRetry());
        vo.setErrorType(task.getErrorType());
        vo.setErrorMsg(task.getErrorMsg());
        DispatchErrorTypeEnum errorType = DispatchErrorTypeEnum.fromCode(task.getErrorType());
        vo.setErrorDesc(errorType == DispatchErrorTypeEnum.NONE ? null : errorType.getDesc());
        vo.setNeedManualFix(errorType.isNeedManualFix());
        vo.setRetryable(errorType.isRetryable());
        vo.setPlatformArticleId(task.getPlatformArticleId());
        vo.setResultUrl(task.getResultUrl());
        vo.setDurationMs(task.getDurationMs());
        vo.setStartTime(task.getStartTime());
        vo.setFinishTime(task.getFinishTime());
        vo.setCreateTime(task.getCreateTime());
        return vo;
    }

    private void skip(DispatchSubmitVO submitVO, String reason) {
        submitVO.getSkippedReasons().add(reason);
    }

    private String generateBatchNo() {
        return "BD" + LocalDateTime.now().format(BATCH_FORMATTER)
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private Long currentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
