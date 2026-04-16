package com.todo.common.core.domain;

import com.todo.common.enums.ResultCode;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 * */
@Data
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    public R() {
        this.timestamp = System.currentTimeMillis();
    }

    public R(Integer code, String message, T data) {
        this.code = code;
        this.msg = message;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // 使用枚举
    public static <T> R<T> ok() {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> R<T> ok(T data, String message) {
        return new R<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> R<T> error(ResultCode resultCode) {
        return new R<>(resultCode.getCode(), resultCode.getMessage(), null);
    }

    public static <T> R<T> error(ResultCode resultCode, String customMessage) {
        return new R<>(resultCode.getCode(), customMessage, null);
    }

    public static <T> R<T> error(Integer code, String message) {
        return new R<>(code, message, null);
    }

    public boolean isOk() {
        return this.code != null && this.code == 200;
    }
}
