package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 任务按完成状态分组的计数
 * */
@Data
public class TodoTaskCountVO {

    /**
     * 是否完成 0-未完成 1-已完成
     * */
    @JsonProperty("completed")
    private Integer completed;

    /**
     * 该状态下的任务数
     * */
    @JsonProperty("total")
    private Long total;
}
