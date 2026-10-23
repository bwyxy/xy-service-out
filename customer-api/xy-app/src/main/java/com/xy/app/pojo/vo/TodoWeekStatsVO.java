package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 本周统计（统计页概览 + 柱状图）
 * */
@Data
public class TodoWeekStatsVO {

    /**
     * 本周第一天 yyyy-MM-dd（周起始按前端偏好，可能为周一或周日）
     * */
    @JsonProperty("startDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    /**
     * 本周最后一天 yyyy-MM-dd
     * */
    @JsonProperty("endDate")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    /**
     * 本周完成总数
     * */
    @JsonProperty("total")
    private Long total;

    /**
     * 完成率 0~100
     * */
    @JsonProperty("rate")
    private Integer rate;

    /**
     * 连续记录天数
     * */
    @JsonProperty("streakDays")
    private Integer streakDays;

    /**
     * 从本周第一天起的 7 天，没有完成记录的日子补 0（柱状图要连续的 7 根）
     * */
    @JsonProperty("days")
    private List<TodoDayCountVO> days;
}
