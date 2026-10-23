package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 番茄专注 · 开始一轮入参
 * */
@Data
public class TodoFocusStartDTO {

    /**
     * 客户端幂等 Id（必传）：同一轮专注重试沿用同一个，
     * 服务端按 (user_id, client_id) 去重，结束时也用它找回这条记录
     * */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * 模式 focus-专注 short-短休息 long-长休息，不传按 focus
     * */
    @JsonProperty("mode")
    private String mode;

    /**
     * 计划秒数，不传按模式默认值（专注 1500 / 短休息 300 / 长休息 900）
     * */
    @JsonProperty("plannedSeconds")
    private Integer plannedSeconds;

    /**
     * 可关联的任务 Id，不传表示这次专注不挂任务
     * */
    @JsonProperty("taskId")
    private Long taskId;
}
