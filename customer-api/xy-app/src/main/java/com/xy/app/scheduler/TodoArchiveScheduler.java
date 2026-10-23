package com.xy.app.scheduler;

import com.xy.app.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 已完成任务自动归档调度器
 *
 * <p>完成的任务先在「已完成」里留一段时间（看得见、方便点错后取消完成），
 * 超过阈值才挪进「已归档」，否则「已完成」分组会越积越长。</p>
 *
 * <p>阈值配成 0 或负数表示关闭自动归档，整轮直接跳过。</p>
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodoArchiveScheduler {

    private final TaskMapper taskMapper;

    /** 已完成任务超过多少天自动归档，0 表示不自动归档 */
    @Value("${todo.archive.auto-days:30}")
    private int autoDays;

    /**
     * 定时把完成时间早于阈值的任务归档
     *
     * <p>一条 update 批量完成，不逐条查；条件是 archived = 0，天然幂等，
     * 多实例同时跑也只是重复执行同一条语句。</p>
     * */
    @Scheduled(fixedDelayString = "${todo.archive.scan-interval-ms:3600000}",
            initialDelayString = "${todo.archive.initial-delay-ms:60000}")
    public void scanAndArchive() {

        if (autoDays <= 0) {
            return;
        }

        LocalDateTime deadline = LocalDateTime.now().minusDays(autoDays);
        int rows = taskMapper.archiveCompletedBefore(deadline);

        if (rows > 0) {
            log.info("自动归档已完成任务 {} 条（完成时间早于 {}）", rows, deadline);
        }
    }
}
