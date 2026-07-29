package com.xy.common.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MessageSendResponse {

    /**
     * 服务器生成的消息 ID
     * */
    @JsonProperty("message_id")
    private Integer messageId;

    /**
     * 消息序列号
     * */
    @JsonProperty("message_seq")
    private Integer messageSeq;

    /**
     * 客户端消息编号（回显）
     * */
    @JsonProperty("client_msg_no")
    private String clientMsgNo;
}
