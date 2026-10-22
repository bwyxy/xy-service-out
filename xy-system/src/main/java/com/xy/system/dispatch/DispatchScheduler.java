package com.xy.system.dispatch;

import com.xy.system.service.blog.IBlogDispatchStatService;
import com.xy.system.service.blog.IBlogDispatchTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分发任务定时调度器
 * 负责兜底扫描待分发任务、自动重试可重试的失败任务、定时拉取各平台运营数据
 * */
@Slf4j
@Component
@ConditionalOnProperty(name = "dispatch.scheduler.enabled", havingValue = "true")
public class DispatchScheduler {

    private final IBlogDispatchTaskService taskService;

    private final IBlogDispatchStatService statService;

    private final ObjectProvider<DispatchTaskExecutor> executorProvider;

    @Value("${dispatch.scan.batch-size:50}")
    private int scanBatchSize;

    public DispatchScheduler(IBlogDispatchTaskService taskService,
                             IBlogDispatchStatService statService,
                             ObjectProvider<DispatchTaskExecutor> executorProvider) {
        this.taskService = taskService;
        this.statService = statService;
        this.executorProvider = executorProvider;
    }

    /**
     * 兜底扫描待分发任务，避免提交时线程池繁忙导致任务滞留
     * */
    @Scheduled(fixedDelayString = "${dispatch.scan.interval-ms:30000}")
    public void scanPendingTasks() {
        try {
            List<Long> taskIds = taskService.selectPendingTaskIds(scanBatchSize);
            for (Long taskId : taskIds) {
                executorProvider.getObject().execute(taskId);
            }
            if (!taskIds.isEmpty()) {
                log.debug("兜底扫描到 {} 条待分发任务", taskIds.size());
            }
        } catch (Exception e) {
            log.error("待分发任务扫描异常", e);
        }
    }

    /**
     * 自动重试瞬时异常的失败任务（网络异常、平台限流、平台异常等）
     * */
    @Scheduled(fixedDelayString = "${dispatch.retry.interval-ms:60000}")
    public void autoRetryFailedTasks() {
        try {
            List<Long> taskIds = taskService.selectRetryableTaskIds(scanBatchSize);
            if (taskIds.isEmpty()) {
                return;
            }
            taskService.retryTasks(taskIds);
            log.info("自动重试 {} 条失败的分发任务", taskIds.size());
        } catch (Exception e) {
            log.error("分发任务自动重试异常", e);
        }
    }

    /**
     * 定时拉取各平台已发布文章的运营数据
     * */
    @Scheduled(cron = "${dispatch.stat.cron:0 0 2 * * ?}")
    public void collectPlatformStats() {
        try {
            int count = statService.refreshStats();
            log.info("分发运营数据采集完成，更新 {} 条记录", count);
        } catch (Exception e) {
            log.error("分发运营数据采集异常", e);
        }
    }
}
