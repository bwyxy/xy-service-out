package com.xy.common.client.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BaseEntity {

    /**
     * 用户 ID
     * */
    private String uid;

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
}
