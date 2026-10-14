package com.xy.common.enums;

import lombok.Getter;

/**
 * 统一状态吗枚举
 * */
@Getter
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    // ==================== 客户端错误 4xx ====================
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    // ==================== 服务端错误 5xx ====================
    INTERNAL_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂时不可用"),

    // ==================== 业务错误 ====================
    BUSINESS_ERROR(1001, "业务异常"),
    DATA_NOT_FOUND(1002, "数据不存在"),
    DATA_ALREADY_EXISTS(1003, "数据已存在"),
    PARAM_VALIDATION_ERROR(1004, "参数校验失败"),
    FILE_UPLOAD_ERROR(1005, "文件上传失败"),
    FILE_DOWNLOAD_ERROR(1006, "文件下载失败"),
    OPERATION_FAILED(1007, "操作失败"),

    // ==================== 登录会话错误 ====================
    /** 会话已失效（被同类型设备登录顶下线 / 已登出 / token 过期） */
    SESSION_EXPIRED(4011, "登录状态已失效，请重新登录");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
