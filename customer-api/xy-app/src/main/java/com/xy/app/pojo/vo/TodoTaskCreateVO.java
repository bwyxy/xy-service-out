package com.xy.app.pojo.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建任务返回
 * */
@Data
public class TodoTaskCreateVO {

    /**
     * 任务 Id
     * */
    private Long taskId;

    /**
     * 服务端算好的提醒时间，无提醒为 null
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime remindAt;

    /**
     * 用户维度同步序号，供前端更新本地水位
     * */
    private Long seq;
}
