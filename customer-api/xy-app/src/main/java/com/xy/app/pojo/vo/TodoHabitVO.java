package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 习惯返回
 * */
@Data
public class TodoHabitVO {

    /**
     * 习惯 Id
     * */
    @JsonProperty("habitId")
    private Long habitId;

    /**
     * 习惯名称
     * */
    @JsonProperty("name")
    private String name;

    /**
     * 图标名（对应前端 y-icon 的 name）
     * */
    @JsonProperty("icon")
    private String icon;

    /**
     * 主题色
     * */
    @JsonProperty("color")
    private String color;

    /**
     * 频率类型 daily-每天 weekly-按周
     * */
    @JsonProperty("freqType")
    private String freqType;

    /**
     * 按周打卡的星期集合，1-周一 ~ 7-周日
     * */
    @JsonProperty("freqDays")
    private List<Integer> freqDays;

    /**
     * 目标天数，0 表示不限
     * */
    @JsonProperty("targetDays")
    private Integer targetDays;

    /**
     * 排序
     * */
    @JsonProperty("sortNo")
    private Integer sortNo;

    /**
     * 状态 1-进行中 0-已结束
     * */
    @JsonProperty("status")
    private Integer status;

    /**
     * 连续打卡天数（今天还没打卡时从昨天往前数，不算断）
     * */
    @JsonProperty("streak")
    private Integer streak;

    /**
     * 本周打卡情况，长度 7，索引 0 为周一
     * */
    @JsonProperty("weekDays")
    private List<Boolean> weekDays;

    /**
     * 创建时间
     * */
    @JsonProperty("createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
