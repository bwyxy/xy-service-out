package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 公共频道参数
 * */
@Data
public class BaseChannel {

    /**
     * 频道 ID
     * */
    @JsonProperty("channel_id")
    private String channelId;

    /**
     * 频道类型
     * */
    @JsonProperty("channel_type")
    private Integer channelType = 2;

    /**
     * 订阅者集合
     * */
    private List<String> subscribers;

    private List<String> uids;
}
