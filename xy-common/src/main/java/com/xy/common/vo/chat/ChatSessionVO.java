package com.xy.common.vo.chat;

import lombok.Data;

/**
 * 客户端用户登录设备（来源：Redis 登录会话）
 *
 * <p>与 fg-chat 的登录会话存储结构对齐：{@code login:session:{token}} / {@code login:user:{uid}}，
 * 相关实现见 fg-chat 的 LoginSessionServiceImpl。</p>
 */
@Data
public class ChatSessionVO {

    /** 会话令牌（强制下线时按该 token 精确踢除） */
    private String token;

    /** 设备类型 1-PC 2-平板 3-手机 */
    private Integer deviceType;

    /** 设备类型名称 */
    private String deviceTypeLabel;

    /** 设备描述（设备类型 / User-Agent 简写） */
    private String deviceName;

    /** 设备唯一标识 */
    private String deviceId;

    /** 登录 IP */
    private String ip;

    /** 登录时间（格式化） */
    private String loginTime;

    /** 最后活跃时间（格式化） */
    private String lastActiveTime;
}
