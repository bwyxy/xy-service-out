package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 任务归档状态入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 *
 * <p>和完成状态一样只传目标状态而不是「切换」：连点两次、弱网重放时按目标状态写入天然幂等。</p>
 * */
@Data
public class TodoTaskArchiveDTO {

    /**
     * 任务 Id
     * */
    @JsonProperty("taskId")
    private Long taskId;

    /**
     * 目标状态 0-取消归档（恢复到已完成） 1-归档
     * */
    @JsonProperty("archived")
    private Integer archived;
}
