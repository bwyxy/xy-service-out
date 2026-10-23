package com.xy.app.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 微信开放接口通用响应
 *
 * <p>access_token 获取与订阅消息下发共用：前者返回 access_token/expires_in，
 * 后者返回 errcode/errmsg/msgid，字段都平铺在同一层级。</p>
 *
 * <p>必须忽略未知字段：微信下发成功时返回的 msgid 曾经不在本类里，
 * Jackson 默认遇未知字段直接抛异常，导致「消息已发出但被判成失败」。</p>
 * */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeChatApiResponse {

    /**
     * 凭证，获取成功时返回
     * */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 凭证有效期（秒）
     * */
    @JsonProperty("expires_in")
    private Long expiresIn;

    /**
     * 错误码，0 表示成功
     * */
    private Integer errcode;

    /**
     * 错误信息
     * */
    private String errmsg;

    /**
     * 订阅消息下发成功时返回的消息 Id
     * */
    private Long msgid;
}
