package com.chat.fg.enums;

import lombok.Getter;

/**
 * 登录设备类型分组
 *
 * <p>同一账号在不同设备类型之间互不影响，可同时保持在线；
 * 同一设备类型下仅允许存在 1 个有效活跃会话（参考微信多端登录规则）。</p>
 */
@Getter
public enum DeviceType {

    PC(1, "PC端"),
    PAD(2, "平板端"),
    PHONE(3, "手机端");

    private final Integer code;
    private final String label;

    DeviceType(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    /**
     * 依据 code 解析设备类型，非法值兜底为 PC
     */
    public static DeviceType fromCode(Integer code) {
        if (code != null) {
            for (DeviceType type : values()) {
                if (type.code.equals(code)) {
                    return type;
                }
            }
        }
        return PC;
    }

    /**
     * 设备类型中文名
     */
    public static String labelOf(Integer code) {
        return fromCode(code).getLabel();
    }

    /**
     * 依据 User-Agent 兜底推断设备类型（客户端未上报 deviceType 时使用）
     */
    public static DeviceType resolveByUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return PC;
        }
        String ua = userAgent.toLowerCase();
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("pad")) {
            return PAD;
        }
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) {
            return PHONE;
        }
        return PC;
    }
}