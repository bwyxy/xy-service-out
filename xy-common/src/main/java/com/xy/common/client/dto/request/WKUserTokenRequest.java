package com.xy.common.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 悟空 IM更新用户token
 * */
@Data
public class WKUserTokenRequest {

    /**
     * 用户 Id
     * */
    private String uid;

    /**
     * 更新时的 token
     * */
    private String token;

    /**
     * 设备标识
     * 0.app 1.web 2. desktop
     * */
    @JsonProperty("device_flag")
    private int deviceFlag;

    /**
     * 设备等级
     * 0.为从设备 1.为主设备
     * */
    @JsonProperty("device_level")
    private int deviceLevel;
}
