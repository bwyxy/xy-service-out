package com.xy.app.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 修改提醒设置的入参（全量覆盖）
 *
 * <p>注意：不接受 user_id，所属用户一律由 token 解析（TodoUserDetails.getCurrentUId）。</p>
 * */
@Data
public class TodoUserSettingUpdateDTO {

    /**
     * 任务提醒开关 0-关 1-开
     * */
    @JsonProperty("taskRemindOn")
    private Integer taskRemindOn;

    /**
     * 每日提醒开关 0-关 1-开
     * */
    @JsonProperty("dailyRemindOn")
    private Integer dailyRemindOn;

    /**
     * 每日提醒时间 HH:mm
     * */
    @JsonProperty("dailyRemindTime")
    private String dailyRemindTime;

    /**
     * 番茄提醒开关 0-关 1-开
     * */
    @JsonProperty("focusRemindOn")
    private Integer focusRemindOn;

    /**
     * 免打扰开始 HH:mm
     * */
    @JsonProperty("quietStart")
    private String quietStart;

    /**
     * 免打扰结束 HH:mm
     * */
    @JsonProperty("quietEnd")
    private String quietEnd;
}
