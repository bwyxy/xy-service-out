package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 日历视图 · 月概览
 * */
@Data
public class TodoMonthVO {

    /**
     * 年份
     * */
    @JsonProperty("year")
    private Integer year;

    /**
     * 月份 1~12
     * */
    @JsonProperty("month")
    private Integer month;

    /**
     * 本月有安排的天数（日历卡片底部「本月有 N 天安排了事情」）
     * */
    @JsonProperty("busyDays")
    private Integer busyDays;

    /**
     * 本月每天的任务数，没有任务的日子不返回（前端点阵只点亮有任务的日子）
     * */
    @JsonProperty("days")
    private List<TodoDayCountVO> days;
}
