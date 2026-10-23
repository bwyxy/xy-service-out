package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 任务完成状态入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 *
 * <p>只传目标状态而不是「切换」：客户端弱网重试、连点两次时，按目标状态写入天然幂等，
 * 不会因为重放把状态又翻回去。</p>
 * */
@Data
public class TodoTaskStatusDTO {

    /**
     * 任务 Id
     * */
    @JsonProperty("taskId")
    private Long taskId;

    /**
     * 目标状态 0-未完成 1-已完成
     * */
    @JsonProperty("completed")
    private Integer completed;
}
