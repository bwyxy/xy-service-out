package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 番茄专注 · 结束一轮入参
 *
 * <p>带上 mode 与 plannedSeconds，是为了兜住 start 请求丢了的场景（弱网下只到了一条 finish）：
 * 找不到原记录时按这两个字段补一条，别让这一轮专注白记。</p>
 * */
@Data
public class TodoFocusFinishDTO {

    /**
     * 客户端幂等 Id（必传），与 start 时为同一轮传的保持一致
     * */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * 结束状态 finished-完整跑完一轮 aborted-中途暂停/重置/切模式/离开页面
     * */
    @JsonProperty("status")
    private String status;

    /**
     * 实际专注秒数（不含暂停时间）
     * */
    @JsonProperty("actualSeconds")
    private Integer actualSeconds;

    /**
     * 模式 focus/short/long，仅在需要补插记录时使用
     * */
    @JsonProperty("mode")
    private String mode;

    /**
     * 计划秒数，仅在需要补插记录时使用
     * */
    @JsonProperty("plannedSeconds")
    private Integer plannedSeconds;
}
