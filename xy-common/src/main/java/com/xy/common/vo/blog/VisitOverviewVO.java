package com.xy.common.vo.blog;

import lombok.Data;

/**
 * 访问统计-概览数据
 */
@Data
public class VisitOverviewVO {

    /**
     * 总浏览量
     */
    private Long totalPv;

    /**
     * 总独立访客
     */
    private Long totalUv;

    /**
     * 今日浏览量
     */
    private Long todayPv;

    /**
     * 今日独立访客
     */
    private Long todayUv;

    /**
     * 昨日浏览量
     */
    private Long yesterdayPv;

    /**
     * 昨日独立访客
     */
    private Long yesterdayUv;

    /**
     * 今日平均停留时长(秒)
     */
    private Double todayAvgDurationSec;
}
