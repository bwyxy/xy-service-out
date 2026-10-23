package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务列表项
 * */
@Data
public class TodoTaskVO {

    /**
     * 任务 Id
     * */
    @JsonProperty("taskId")
    private Long taskId;

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
     * 优先级 0-无 1-普通 2-重要（列表页小旗子）
     * */
    @JsonProperty("priority")
    private Integer priority;

    /**
     * 紧急程度 1~4（四象限）
     * */
    @JsonProperty("urgencyLevel")
    private Integer urgencyLevel;

    /**
     * 是否完成 0-未完成 1-已完成
     * */
    @JsonProperty("completed")
    private Integer completed;

    /**
     * 完成时间（已完成与归档页记录行的达成时间）
     * */
    @JsonProperty("completionTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime completionTime;

    /**
     * 是否归档 0-否 1-是
     * */
    @JsonProperty("archived")
    private Integer archived;

    /**
     * 归档时间（已归档页记录行的时间）
     * */
    @JsonProperty("archiveTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime archiveTime;

    /**
     * 截止时间
     * */
    @JsonProperty("dueDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    /**
     * 重复规则
     * */
    @JsonProperty("repeatRule")
    private String repeatRule;

    /**
     * 提前提醒分钟数，0-不提醒
     * */
    @JsonProperty("remindBefore")
    private Integer remindBefore;

    /**
     * 用户维度同步序号
     * */
    @JsonProperty("seq")
    private Long seq;
}
