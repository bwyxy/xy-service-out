package com.chat.fg.task;

import com.chat.fg.service.IRtcService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * RTC 呼叫超时清理任务
 *
 * <p>悟空 IM 的实时信令（noPersist）仅在线可达，被叫离线/未响应时
 * 房间会一直停留在"呼叫中"。本任务周期性将超时房间置为结束，
 * 并通知双方取消、落"未接听"通话记录。</p>
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class RtcTimeoutTask {

    private final IRtcService rtcService;

    /**
     * 每 10 秒扫描一次超时房间
     * */
    @Scheduled(fixedDelay = 10_000)
    public void expireStaleCalls() {
        try {
            int count = rtcService.expireStaleRooms();
            if (count > 0) {
                log.info("RTC 超时房间清理完成, 本轮处理 {} 个", count);
            }
        } catch (Exception e) {
            log.error("RTC 超时房间清理失败", e);
        }
    }
}
