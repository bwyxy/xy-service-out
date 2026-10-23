package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 套用模板入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 * */
@Data
public class TodoTemplateApplyDTO {

    /**
     * 客户端幂等批次号，同一批次重复提交只会建一次清单和任务
     * */
    @JsonProperty("batchId")
    private String batchId;
}
