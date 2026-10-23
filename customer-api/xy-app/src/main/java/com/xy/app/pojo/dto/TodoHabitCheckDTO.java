package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 习惯打卡入参
 * */
@Data
public class TodoHabitCheckDTO {

    /**
     * 打卡日期 yyyy-MM-dd，不传表示今天
     * */
    @JsonProperty("logDate")
    private String logDate;
}
