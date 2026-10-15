package com.xy.common.enums;

import lombok.Getter;

/**
 * 平台正文格式枚举
 * 不同平台接收的正文格式不同，由适配器声明，内容适配层据此产出对应正文
 * */
@Getter
public enum DispatchContentFormat {

    /** Markdown 正文 */
    MARKDOWN("markdown", "Markdown"),

    /** HTML 富文本正文 */
    HTML("html", "HTML富文本"),

    /** 纯文本正文 */
    PLAIN_TEXT("plainText", "纯文本");

    private final String code;

    private final String desc;

    DispatchContentFormat(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
