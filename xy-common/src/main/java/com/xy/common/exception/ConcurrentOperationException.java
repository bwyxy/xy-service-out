package com.xy.common.exception;

/**
 * 并发操作异常
 * */
public class ConcurrentOperationException extends BusinessException {

    public ConcurrentOperationException(String message) {
        super(message);
    }
}
