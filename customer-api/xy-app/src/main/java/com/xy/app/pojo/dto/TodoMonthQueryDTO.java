package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 日历视图查询入参
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 * */
@Data
public class TodoMonthQueryDTO {

    /**
     * 月份，格式 yyyy-MM
     * */
    @JsonProperty("month")
    private String month;
}
