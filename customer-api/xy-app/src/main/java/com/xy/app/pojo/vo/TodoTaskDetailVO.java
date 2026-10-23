package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务详情返回
 *
 * <p>比列表项多了清单名、创建时间与下次提醒时间——详情页要把这条任务讲清楚，
 * 列表里为了省一次 join 只带 tagId，详情页只有一条记录，多查一次清单表没有压力。</p>
 * */
@Data
public class TodoTaskDetailVO {

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
     * 所属清单名，未归类或清单已删时为「未归类」
     * */
    @JsonProperty("tagName")
    private String tagName;

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
     * 是否完成 0-未完成 1-已完成
     * */
    @JsonProperty("completed")
    private Integer completed;

    /**
     * 完成时间
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
     * 归档时间
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
     * 重复规则 DAILY / WEEKLY / MONTHLY，空串表示不重复
     * */
    @JsonProperty("repeatRule")
    private String repeatRule;

    /**
     * 提前提醒分钟数，0-不提醒
     * */
    @JsonProperty("remindBefore")
    private Integer remindBefore;

    /**
     * 下次提醒时间（服务端算好，没设提醒时为 null）
     * */
    @JsonProperty("remindAt")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindAt;

    /**
     * 创建时间
     * */
    @JsonProperty("createTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
