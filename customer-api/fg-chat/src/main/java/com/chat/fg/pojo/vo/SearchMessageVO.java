package com.chat.fg.pojo.vo;

import lombok.Data;

/**
 * 聊天记录搜索结果
 */
@Data
public class SearchMessageVO {

    /**
     * 消息序号（前端据此定位到原消息）
     */
    private Long messageSeq;

    /**
     * 消息 ID
     */
    private Long messageId;

    /**
     * 发送者用户 ID
     */
    private String fromUid;

    /**
     * 消息类型
     */
    private Integer type;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息时间戳（毫秒）
     */
    private Long timestamp;
}
