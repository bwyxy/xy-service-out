package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 频道历史消息
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MsgRequest {

    /**
     * 当前登录用户 ID
     * */
    @JsonProperty("login_uid")
    private String loginUid;

    /**
     * 频道 ID
     * */
    @JsonProperty("channel_id")
    private String channelId;

    /**
     * 频道类型 (1=个人频道, 2=群组频道)
     * */
    @JsonProperty("channel_type")
    private Integer channelType;

    /**
     * 起始消息序号（包含）
     * */
    @JsonProperty("start_message_seq")
    private Integer startMessageSeq;

    /**
     * 结束消息序号（不包含）
     * */
    @JsonProperty("end_message_seq")
    private Integer endMessageSeq;

    /**
     * 返回消息数量限制，最大 10000
     * */
    @JsonProperty("limit")
    private Integer limit;

    /**
     * 拉取模式 (0=向下拉取, 1=向上拉取)
     * */
    @JsonProperty("pull_mode")
    private Integer pullMode = 0;
}
