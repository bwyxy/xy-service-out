package com.xy.common.vo.blog;

import lombok.Data;

import java.time.LocalDate;

/**
 * 访问统计-每日汇总数据
 */
@Data
public class VisitDailyStatsVO {

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 浏览量(PV)
     */
    private Long pv;

    /**
     * 独立访客(UV)
     */
    private Long uv;

    /**
     * 会话数
     */
    private Long sessionCount;

    /**
     * 停留时长采样次数
     */
    private Long durationCount;

    /**
     * 停留时长总和(毫秒)
     */
    private Long totalDurationMs;

    /**
     * 平均停留时长(秒)
     */
    private Double avgDurationSec;
}
