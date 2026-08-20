package com.chat.fg.pojo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MsgDTO {

    @NotBlank(message = "频道 Id不能为空")
    private String channelId;

    @NotNull(message = "频道类型不能为空 1-个人频道 2-群组频道")
    private Integer channelType;

    /**
     * 起始消息序号（包含）
     * */
    private Integer startMessageSeq = 0;

    /**
     * 结束消息序号（不包含）
     * */
    private Integer endMessageSeq = 0;

    /**
     * 返回消息数量限制，最大 10000
     * */
    private Integer limit = 0;

    /**
     * 拉取模式 (0=向下拉取, 1=向上拉取)
     * */
    private Integer pullMode = 0;
}
