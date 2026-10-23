package com.xy.app.pojo.vo;

import lombok.Data;

/**
 * 用户提醒相关偏好
 *
 * <p>只暴露提醒设置页用得到的字段，主题/字号/同步等由各自的页面另接。</p>
 * */
@Data
public class TodoUserSettingVO {

    /**
     * 任务提醒开关 0-关 1-开
     * */
    private Integer taskRemindOn;

    /**
     * 每日提醒开关 0-关 1-开
     * */
    private Integer dailyRemindOn;

    /**
     * 每日提醒时间 HH:mm
     * */
    private String dailyRemindTime;

    /**
     * 番茄提醒开关 0-关 1-开
     * */
    private Integer focusRemindOn;

    /**
     * 免打扰开始 HH:mm
     * */
    private String quietStart;

    /**
     * 免打扰结束 HH:mm
     * */
    private String quietEnd;
}
