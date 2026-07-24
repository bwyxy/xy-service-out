package com.xy.app.response;

import lombok.Data;

/**
 * 微信 jscode2session 接口返回的 JSON 映射
 * */
@Data
public class WechatSessionResponse {

    /** 用户唯一 Id */
    private String openid;

    /** 会话密钥 */
    private String session_key;

    /** 状态 */
    private Integer errcode;

    /** 消息 */
    private String errmsg;
}
