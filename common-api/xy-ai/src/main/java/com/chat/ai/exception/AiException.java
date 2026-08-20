package com.chat.ai.exception;

import com.xy.common.exception.ServiceException;

/**
 * AI 模块业务异常
 */
public class AiException extends ServiceException {

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
