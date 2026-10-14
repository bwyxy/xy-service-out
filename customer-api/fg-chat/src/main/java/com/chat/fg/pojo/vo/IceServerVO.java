package com.chat.fg.pojo.vo;

import lombok.Data;

/**
 * ICE 服务器配置 VO
 */
@Data
public class IceServerVO {

    /**
     * 服务器地址列表（stun:/turn: 前缀）
     */
    private String urls;

    /**
     * TURN 用户名（stun 时为空）
     */
    private String username;

    /**
     * TURN 凭证（stun 时为空）
     */
    private String credential;
}
