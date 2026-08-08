package com.xy.common.enums;

import com.xy.common.exception.BusinessException;
import lombok.Getter;

/**
 * 频道类型 枚举
 * */
@Getter
public enum ChannelType {

    PERSONAL_CHANNEL(1, "个人频道"),

    GROUP_CHANNEL(2, "群组频道");

    private final Integer code;

    private final String name;
    ChannelType(Integer code, String name) {
        this.code = code;
        this.name = name;
    }

    public static ChannelType fromCode(Integer code) {
        for (ChannelType channelType : ChannelType.values()) {
            if (channelType.code.equals(code)) {
                return channelType;
            }
        }
        return null;
    };

}
