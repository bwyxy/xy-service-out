package com.todo.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 待办任务 dto
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToDoTaskDTO {

    /**
     * 任务 Id
     * */
    @JsonProperty("taskId")
    private Long taskId;

    /**
     * 任务标题
     * */
    @NotNull(message = "任务标题不能为 null")
    @NotBlank(message = "任务标题不能为空")
    @Size(min = 4, max = 255, message = "任务标题在4~255之间")
    @JsonProperty("taskTitle")
    private String taskTitle;

    /**
     * 描述
     * */
    @NotNull(message = "描述不能为 null")
    @JsonProperty("description")
    private String description;

    /**
     * 紧急程度
     * */
    @NotNull(message = "紧急程度不能为空")
    @Size(min = 1, max = 4, message = "紧急程度在1～4")
    @JsonProperty("urgencyLevel")
    private int urgencyLevel;

    /**
     * 开始时间
     * */
    @Future(message = "开始时间必须是未来时间")
    @JsonProperty("startTime")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     * */
    @Future(message = "截止时间必须是未来时间")
    @NotNull(message = "结束时间不能为空")
    @JsonProperty("dueDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dueDate;

    /**
     * 标签 ID
     * */
    @JsonProperty("tagIds")
    private List<Long> tagIds;
}
