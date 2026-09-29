package com.xy.common.dto.blog;

import lombok.Data;

import java.time.LocalDate;

/**
 * 访问统计查询参数
 */
@Data
public class VisitStatsQuery {

    /**
     * 开始日期
     */
    private LocalDate startDate;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 来源筛选
     */
    private String source;

    /**
     * 页面路径筛选
     */
    private String path;

    /**
     * 排行数量, 默认 10
     */
    private Integer topN = 10;

    public Integer getTopN() {
        if (topN == null || topN <= 0) {
            return 10;
        }
        return topN;
    }
}
