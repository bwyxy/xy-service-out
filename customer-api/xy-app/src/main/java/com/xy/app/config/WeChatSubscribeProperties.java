package com.xy.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信订阅消息配置
 *
 * <p>模板 Id 与各字段 key 全部走配置：换模板时只改 application.yml，不动代码。
 * 字段 key（thing1/time2/...）必须与小程序后台模板详情里的关键词完全一致，
 * 对不上时微信会直接返回 47003。</p>
 * */
@Data
@Component
@ConfigurationProperties(prefix = "wechat.subscribe")
public class WeChatSubscribeProperties {

    /**
     * 任务提醒模板 Id
     * */
    private String reminderTemplateId = "";

    /**
     * 点击消息后的跳转页，不带前导斜杠
     * */
    private String page = "pages/list/index";

    /**
     * 温馨提示字段的固定文案
     * */
    private String tipText = "到点啦，记得完成今天的安排";

    /**
     * 活动事件（任务标题）字段 key
     * */
    private String titleKey = "thing1";

    /**
     * 开始时间（任务截止时间）字段 key
     * */
    private String startTimeKey = "time2";

    /**
     * 备忘事项（任务备注）字段 key
     * */
    private String memoKey = "thing3";

    /**
     * 温馨提示字段 key
     * */
    private String tipKey = "thing5";

    /**
     * 提醒时间字段 key
     * */
    private String remindTimeKey = "time8";
}
