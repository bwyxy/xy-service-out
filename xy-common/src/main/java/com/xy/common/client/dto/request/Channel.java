package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 创建更新频道
 * */
@Data
@EqualsAndHashCode(callSuper = true)
public class Channel extends BaseChannel {

    /**
     * 是否封禁频道
     * */
    private Integer ban = 0;
}
