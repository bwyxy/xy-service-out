package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建任务入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 * */
@Data
public class TodoTaskCreateDTO {

    /**
     * 客户端幂等 Id，弱网重试时保证不重复建任务
     * */
    @JsonProperty("clientId")
    private String clientId;

    /**
     * 任务标题
     * */
    @JsonProperty("title")
    private String title;

    /**
     * 任务描述（备注）
     * */
    @JsonProperty("description")
    private String description;

    /**
     * 所属清单 Id
     * */
    @JsonProperty("tagId")
    private Long tagId;

    /**
     * 优先级 0-无 1-普通 2-重要
     * */
    @JsonProperty("priority")
    private Integer priority;

    /**
     * 紧急程度 1~4（四象限）
     * */
    @JsonProperty("urgencyLevel")
    private Integer urgencyLevel;

    /**
     * 截止时间
     * */
    @JsonProperty("dueDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    /**
     * 重复规则 DAILY / WEEKLY:1,3 / FREQ=WEEKLY;BYDAY=MO
     * */
    @JsonProperty("repeatRule")
    private String repeatRule;

    /**
     * 提前提醒分钟数，0-不提醒
     * */
    @JsonProperty("remindBefore")
    private Integer remindBefore;
}
