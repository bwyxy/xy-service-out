package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.xy.common.client.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 清除未读消息
 * */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClearUnread extends BaseEntity {

    /**
     * 消息序列号，指定清除到哪条消息为止
     * */
    @JsonProperty("message_seq")
    private Integer messageSeq;
}
