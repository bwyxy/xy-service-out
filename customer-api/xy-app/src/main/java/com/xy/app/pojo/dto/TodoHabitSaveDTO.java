package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 习惯新增 / 修改入参
 * */
@Data
public class TodoHabitSaveDTO {

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
     * 按周打卡的星期集合，1-周一 ~ 7-周日；daily 时忽略
     * */
    @JsonProperty("freqDays")
    private List<Integer> freqDays;

    /**
     * 目标天数，0 或不传表示不限
     * */
    @JsonProperty("targetDays")
    private Integer targetDays;

    /**
     * 排序，新增时不传则排到当前末尾
     * */
    @JsonProperty("sortNo")
    private Integer sortNo;
}
