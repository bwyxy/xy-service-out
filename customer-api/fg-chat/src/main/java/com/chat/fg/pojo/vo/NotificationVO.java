package com.chat.fg.pojo.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通知 VO
 * */
@Data
public class NotificationVO {

    /**
     * 通知ID
     * */
    private Long id;

    /**
     * 通知类型 1-好友请求 2-好友同意 3-系统通知
     * */
    private Integer type;

    /**
     * 类型描述
     * */
    private String typeDesc;

    /**
     * 标题
     * */
    private String title;

    /**
     * 内容
     * */
    private String content;

    /**
     * 关联ID
     * */
    private Long relatedId;

    /**
     * 是否已读 0-未读 1-已读
     * */
    private Integer isRead;

    /**
     * 扩展信息
     * */
    private String extra;

    /**
     * 创建时间
     * */
    private LocalDateTime createTime;
}
