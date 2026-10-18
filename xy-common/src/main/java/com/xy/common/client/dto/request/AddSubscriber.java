package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 添加订阅者
 * */
@Data
@EqualsAndHashCode(callSuper = true)
public class AddSubscriber extends BaseChannel {

    /**
     * 是否重置订阅者 （0.不重置 1.重置）
     * */
    private Integer reset = 0;

    /**
     * 是否为临时频道 0.否 1.是 临时频道的订阅者将在下次重启后自动删除
     * */
    @JsonProperty("temp_subscriber")
    private Integer tempSubscriber = 0;
}
