package com.xy.common.enums;

import lombok.Getter;

/**
 * 分发异常类型枚举
 * 用于把第三方平台的各类异常归一化，便于前端给出明确提示与判断是否可自动重试
 * */
@Getter
public enum DispatchErrorTypeEnum {

    /** 无异常 */
    NONE("NONE", "无异常", false, false),

    /** 凭证未配置或无效 */
    CREDENTIAL_INVALID("CREDENTIAL_INVALID", "平台凭证未配置或无效，请检查账号配置", false, true),

    /** 凭证已过期 */
    CREDENTIAL_EXPIRED("CREDENTIAL_EXPIRED", "平台凭证已过期，请重新配置账号凭证", true, true),

    /** 触发平台限流 */
    RATE_LIMITED("RATE_LIMITED", "触发平台接口限流，请稍后重试", true, false),

    /** 内容违规 */
    CONTENT_VIOLATION("CONTENT_VIOLATION", "内容未通过平台审核，请修改后重试", false, true),

    /** 内容超长 */
    CONTENT_TOO_LARGE("CONTENT_TOO_LARGE", "内容长度超出平台限制，请精简后重试", false, true),

    /** 内容重复 */
    DUPLICATE_CONTENT("DUPLICATE_CONTENT", "平台已存在相同内容，请勿重复分发", false, true),

    /** 网络异常 */
    NETWORK_ERROR("NETWORK_ERROR", "调用平台接口网络异常，请稍后重试", true, false),

    /** 平台接口异常 */
    PLATFORM_ERROR("PLATFORM_ERROR", "平台接口返回异常，请稍后重试或联系平台", true, false),

    /** 未找到适配器 */
    ADAPTER_NOT_FOUND("ADAPTER_NOT_FOUND", "未找到该平台的适配器实现", false, true),

    /** 平台未启用 */
    PLATFORM_DISABLED("PLATFORM_DISABLED", "该分发平台未启用，请先在平台配置中开启", false, true),

    /** 无平台使用权限 */
    PERMISSION_DENIED("PERMISSION_DENIED", "当前账号无权使用该分发平台", false, true),

    /** 任务并发冲突 */
    CONCURRENT_CONFLICT("CONCURRENT_CONFLICT", "任务正在执行中，请勿重复提交", true, false),

    /** 未知异常 */
    UNKNOWN("UNKNOWN", "分发失败，未知异常", true, false);

    private final String code;

    private final String desc;

    /** 是否属于可自动重试的瞬时异常 */
    private final boolean retryable;

    /** 是否需要用户人工介入（如重新配置凭证、修改内容） */
    private final boolean needManualFix;

    DispatchErrorTypeEnum(String code, String desc, boolean retryable, boolean needManualFix) {
        this.code = code;
        this.desc = desc;
        this.retryable = retryable;
        this.needManualFix = needManualFix;
    }

    /**
     * 根据编码解析异常类型
     * */
    public static DispatchErrorTypeEnum fromCode(String code) {
        if (code == null || code.isEmpty()) {
            return NONE;
        }
        for (DispatchErrorTypeEnum item : values()) {
            if (item.code.equals(code)) {
                return item;
            }
        }
        return UNKNOWN;
    }
}
