package com.xy.common.enums;

import lombok.Getter;

/**
 * 分发平台枚举
 * 用于适配器注册编码与平台默认限制，实际可用性以 blog_dispatch_platform 配置为准
 * */
@Getter
public enum DispatchPlatformEnum {

    /** 掘金 */
    JUEJIN("juejin", "掘金"),

    /** CSDN */
    CSDN("csdn", "CSDN"),

    /** 知乎 */
    ZHIHU("zhihu", "知乎"),

    /** 微信公众平台 */
    WECHAT("wechat", "微信公众平台"),

    /** 简书 */
    JIANSHU("jianshu", "简书"),

    /** 开源中国 */
    OSCHINA("oschina", "开源中国");

    private final String code;

    private final String name;

    DispatchPlatformEnum(String code, String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * 根据编码解析平台枚举
     * @param code 平台编码
     * @return 匹配的枚举，未匹配返回 null
     * */
    public static DispatchPlatformEnum fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (DispatchPlatformEnum item : values()) {
            if (item.code.equalsIgnoreCase(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 校验编码是否为受支持的平台
     * */
    public static boolean isSupported(String code) {
        return fromCode(code) != null;
    }
}
