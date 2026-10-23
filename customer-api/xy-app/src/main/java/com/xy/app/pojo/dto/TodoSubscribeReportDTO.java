package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 订阅授权结果上报入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 *
 * <p>一次性订阅的语义是「授权一次 = 可下发一条」，所以前端每弹一次授权就上报一次，
 * count 是本次实际拿到 accept 的模板个数（勾了「总是保持以上选择」时也会返回 accept，照报）。</p>
 * */
@Data
public class TodoSubscribeReportDTO {

    /**
     * 模板 Id
     * */
    @JsonProperty("templateId")
    private String templateId;

    /**
     * 本次新增的授权次数
     * */
    @JsonProperty("count")
    private Integer count;
}
