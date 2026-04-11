package com.todo.common.base;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一响应结果
 * */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code;
    private String msg;
    private T data;
    private Long timestamp;

    public R(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
        this.timestamp = System.currentTimeMillis();
    }

    public static <T> R<T> ok() {
        return new R<>(200, "操作成功");
    }

    public static <T> R<T> ok(T data) {
        return new R<>(200, "操作成功", data, System.currentTimeMillis());
    }

    public static <T> R<T> ok(String msg, T data) {
        return new R<>(200, msg, data, System.currentTimeMillis());
    }

    public static <T> R<T> error(String msg) {
        return new R<>(500, msg, null, System.currentTimeMillis());
    }

    public static <T> R<T> error(Integer code, String msg) {
        return new R<>(code, msg, null, System.currentTimeMillis());
    }
}
