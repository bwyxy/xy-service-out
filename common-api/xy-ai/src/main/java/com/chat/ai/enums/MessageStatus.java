package com.chat.ai.enums;

import lombok.Getter;

/**
 * 消息状态
 */
@Getter
public enum MessageStatus {

    COMPLETED(0, "已完成"),
    GENERATING(1, "生成中"),
    FAILED(2, "失败");

    private final int code;
    private final String desc;

    MessageStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
