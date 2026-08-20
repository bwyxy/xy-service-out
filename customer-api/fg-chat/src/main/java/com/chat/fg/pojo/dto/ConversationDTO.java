package com.chat.fg.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConversationDTO {

    /**
     * 每个会话返回的最近消息数量
     * 默认1 条
     * */
    private Integer msgCount = 1;

    /**
     * 客户端最后消息序列号，格式：channelID:channelType:last_msg_seq|channelID:channelType:last_msg_seq
     * */
    private String lastMsgSeqs;

    /**
     * 是否只返回未读会话 (1=只返回未读, 0=返回所有)
     * */
    private Integer onlyUnread = 0;

    /**
     * 版本时间戳，用于增量同步
     * */
    private Long version;

    /**
     * 要排除的频道类型数组
     * */
    private List<Integer> excludeChannelTypes;
}
