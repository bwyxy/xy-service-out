package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 番茄专注 · 今日统计返回
 * */
@Data
public class TodoFocusTodayVO {

    /**
     * 今日完成的番茄数（只算完整跑完的 focus 轮，休息轮次不计）
     * */
    @JsonProperty("todayCount")
    private Integer todayCount;

    /**
     * 今日累计专注分钟（按秒四舍五入）
     * */
    @JsonProperty("todayMinutes")
    private Integer todayMinutes;

    /**
     * 昨日完成的番茄数，用于「比昨天多几个」
     * */
    @JsonProperty("yesterdayCount")
    private Integer yesterdayCount;
}
