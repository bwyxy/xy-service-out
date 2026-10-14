package com.chat.fg.pojo;

import lombok.Data;

/**
 * 登录会话记录
 *
 * <p>每条会话关联：用户 ID、设备类型、设备唯一标识、登录时间、会话令牌、
 * 最后活跃时间、会话状态，保证会话状态可追溯、可管控。</p>
 */
@Data
public class LoginSession {

    /** 用户 ID */
    private Long uid;

    /** 设备类型分组：1=PC端 2=平板端 3=手机端 */
    private Integer deviceType;

    /** 设备唯一标识（客户端本地持久化的设备指纹） */
    private String deviceId;

    /** 设备描述（平台 / User-Agent 简写） */
    private String deviceName;

    /** 会话令牌（JWT） */
    private String token;

    /** 登录时间（epoch millis） */
    private Long loginTime;

    /** 最后活跃时间（epoch millis） */
    private Long lastActiveTime;

    /** 会话状态：ACTIVE / KICKED / LOGOUT */
    private String status;

    /** 登录 IP */
    private String ip;
}