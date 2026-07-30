package com.xy.common.client.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xy.common.client.entity.Message;
import lombok.Data;

import java.util.List;

/**
 * 会话
 * */
@Data
public class ConversationVO {

    /**
     * 频道 ID
     * */
    @JsonProperty("channel_id")
    private String channelId;

    /**
     * 频道类型
     * 1 - 个人频道
     * 2 - 群组频道
     * */
    @JsonProperty("channel_type")
    private Integer channelType;

    /**
     * 未读消息数量
     * */
    private Integer unread;

    /**
     * 最后消息时间戳
     * */
    private Integer timestamp;

    /**
     * 最后消息序列号
     * */
    @JsonProperty("last_msg_seq")
    private Integer lastMsgSeq;

    @JsonProperty("last_client_msg_no")
    private String lastClientMsgNo;

    @JsonProperty("offset_msg_seq")
    private Integer offsetMsgSeq;

    @JsonProperty("readed_to_msg_seq")
    private Integer readedToMsgSeq;

    /**
     * 会话版本号（纳秒级时间戳）
     * */
    private Long version;

    private List<Message> recents;
}
