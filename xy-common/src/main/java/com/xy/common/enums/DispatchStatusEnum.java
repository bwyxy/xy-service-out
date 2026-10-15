package com.xy.common.enums;

import lombok.Getter;

/**
 * 分发任务状态枚举
 * */
@Getter
public enum DispatchStatusEnum {

    /** 待分发 */
    PENDING(0, "待分发"),

    /** 分发中 */
    RUNNING(1, "分发中"),

    /** 分发成功 */
    SUCCESS(2, "分发成功"),

    /** 分发失败 */
    FAILED(3, "分发失败");

    private final Integer code;

    private final String desc;

    DispatchStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码解析状态
     * */
    public static DispatchStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (DispatchStatusEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取状态中文描述，未匹配返回空串
     * */
    public static String descOf(Integer code) {
        DispatchStatusEnum status = fromCode(code);
        return status == null ? "" : status.getDesc();
    }
}
