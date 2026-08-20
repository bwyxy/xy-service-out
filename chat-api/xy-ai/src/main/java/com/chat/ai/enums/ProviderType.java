package com.chat.ai.enums;

import lombok.Getter;

/**
 * AI 模型提供者类型
 */
@Getter
public enum ProviderType {

    OPENAI_COMPATIBLE("OPENAI_COMPATIBLE", "OpenAI 兼容接口"),
    DASHSCOPE("DASHSCOPE", "阿里云百炼"),
    QIANFAN("QIANFAN", "百度千帆");

    private final String code;
    private final String desc;

    ProviderType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ProviderType of(String code) {
        for (ProviderType value : values()) {
            if (value.code.equalsIgnoreCase(code)) {
                return value;
            }
        }
        return OPENAI_COMPATIBLE;
    }
}
