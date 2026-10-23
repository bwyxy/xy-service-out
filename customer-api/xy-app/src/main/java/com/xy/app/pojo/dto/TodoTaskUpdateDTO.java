package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新任务入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 *
 * <p>全量覆盖语义：没传的字段按清空处理。编辑页会把整套字段带上来，与创建时提交的是同一套，
 * 所以不做「只改传了的字段」的部分更新——那样反而更容易漏字段。</p>
 * */
@Data
public class TodoTaskUpdateDTO {

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
     * 所属清单 Id，传 null 表示改为未归类
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
     * 重复规则 DAILY / WEEKLY / MONTHLY，空串表示不重复
     * */
    @JsonProperty("repeatRule")
    private String repeatRule;

    /**
     * 提前提醒分钟数，0-不提醒
     * */
    @JsonProperty("remindBefore")
    private Integer remindBefore;
}
