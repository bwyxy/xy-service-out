package com.xy.common.client.entity;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 消息
 * */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Message {

    /**
     * setting
     * */
    private Integer setting;
    /**
     * 消息 ID
     * */
    @JsonAlias("message_id")
    private Long messageId;

    @JsonAlias("message_idstr")
    private String messageIdStr;

    /**
     * 消息序列号
     * */
    @JsonAlias("message_seq")
    private Long messageSeq;

    /**
     * 客户端消息编号
     * */
    @JsonAlias("client_msg_no")
    private String clientMsgNo;

    @JsonAlias("stream_idstr")
    private String streamIdStr;

    /**
     * 发送者用户 ID
     * */
    @JsonAlias("from_uid")
    private String fromUid;

    @JsonAlias("channel_id")
    private String channelId;

    @JsonAlias("channel_type")
    private Integer channelType;

    private Integer expire;

    /**
     * 消息时间戳
     * */
    private Integer timestamp;

    /**
     * Base64 编码的消息内容
     * */
    private String payload;

    /**
     * 消息头部信息
     * */
    private MessageHeader header;

    @Data
    public static class MessageHeader {

        /**
         * 是否不持久化消息 (0=持久化, 1=不持久化)
         * */
        @JsonAlias("no_persist")
        private Integer noPersist;

        /**
         * 是否显示红点通知 (0=不显示, 1=显示)
         * */
        @JsonAlias("red_dot")
        private Integer redDot;

        /**
         * 是否是写扩散，这里一般是0，只有cmd消息才是1
         * */
        @JsonAlias("sync_once")
        private Integer syncOnce;
    }
}
