package com.xy.common.client.vo;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xy.common.client.entity.Message;
import lombok.Data;

import java.util.List;

/**
 * 消息列表
 * */
@Data
public class MessageVO {

    /**
     * 起始消息序号
     * */
    @JsonAlias("start_message_seq")
    private Integer startMessageSeq;

    /**
     * 结束消息序号
     * */
    @JsonAlias("end_message_seq")
    private Integer endMessageSeq;

    /**
     * 拉取模式 (0=向下拉取, 1=向上拉取)
     * */
    private Integer more;

    /**
     * 消息列表
     * */
    private List<Message> messages;
}
