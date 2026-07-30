package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class ConversationRequest {

    /**
     * 用户 ID
     * */
    private String uid;

    /**
     * 版本时间戳，用于增量同步
     * */
    private Long version;

    /**
     * 客户端最后消息序列号，格式：channelID:channelType:last_msg_seq|channelID:channelType:last_msg_seq
     * */
    @JsonProperty("last_msg_seqs")
    private String lastMsgSeqs;

    /**
     * 每个会话返回的最近消息数量
     * */
    @JsonProperty("msg_count")
    private Integer msgCount;

    /**
     * 是否只返回未读会话 (1=只返回未读, 0=返回所有)
     * */
    @JsonProperty("only_unread")
    private Integer onlyUnread;

    /**
     * 要排除的频道类型数组
     * */
    @JsonProperty("exclude_channel_types")
    private List<Integer> excludeChannelTypes;
}
