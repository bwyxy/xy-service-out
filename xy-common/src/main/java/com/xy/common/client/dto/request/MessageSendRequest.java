package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 发送消息
 * */
@Data
public class MessageSendRequest {

    /**
     * Base64 编码的消息内容
     * */
    private String payload;

    /**
     * 发送者用户 ID
     * */
    @JsonProperty("from_uid")
    private String fromUid;

    /**
     * 目标频道 ID
     * */
    @JsonProperty("channel_id")
    private String channelId;

    /**
     * 频道类型 (1=个人频道, 2=群组频道)
     * */
    @JsonProperty("channel_type")
    private Integer channelType;

    /**
     * 消息头部信息
     * */
    private MessageHeader header;

    /**
     * 客户端消息编号，用于去重和状态跟踪
     * */
    @JsonProperty("client_msg_no")
    private String clientMsgNo;

    /**
     * 流消息编号
     * */
    @JsonProperty("stream_no")
    private String streamNo;

    /**
     * 消息过期时间（秒），0 表示不过期
     * */
    private Integer expire;

    /** 订阅者用户 ID
     * 指定接收消息的订阅者列表（此参数只有CMD消息才有效）
     * */
    private List<String> subscribers;

    @Data
    public static class MessageHeader {

        /**
         * 是否不持久化消息 (0=持久化, 1=不持久化)
         * */
        @JsonProperty("no_persist")
        private Integer noPersist;

        /**
         * 是否显示红点通知 (0=不显示, 1=显示)
         * */
        @JsonProperty("red_dot")
        private Integer redDot;

        /**
         * 是否是写扩散，这里一般是0，只有cmd消息才是1
         * */
        @JsonProperty("sync_once")
        private Integer syncOnce;
    }
}
