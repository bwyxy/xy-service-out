package com.xy.app.pojo.vo;

import lombok.Data;

/**
 * 订阅消息剩余授权额度
 * */
@Data
public class TodoNotifyQuotaVO {

    /**
     * 模板 Id
     * */
    private String templateId;

    /**
     * 剩余可下发条数
     * */
    private Integer remain;

    /**
     * 累计获得授权次数
     * */
    private Integer total;
}
