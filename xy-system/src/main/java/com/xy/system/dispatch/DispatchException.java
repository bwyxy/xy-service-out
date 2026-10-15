package com.xy.system.dispatch;

import com.xy.common.enums.DispatchErrorTypeEnum;
import lombok.Getter;

/**
 * 分发业务异常
 * 携带归一化后的错误类型，便于任务落库与前端提示
 * */
@Getter
public class DispatchException extends RuntimeException {

    private final DispatchErrorTypeEnum errorType;

    public DispatchException(DispatchErrorTypeEnum errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public DispatchException(DispatchErrorTypeEnum errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    public static DispatchException credentialInvalid(String message) {
        return new DispatchException(DispatchErrorTypeEnum.CREDENTIAL_INVALID, message);
    }

    public static DispatchException credentialExpired(String message) {
        return new DispatchException(DispatchErrorTypeEnum.CREDENTIAL_EXPIRED, message);
    }

    public static DispatchException rateLimited(String message) {
        return new DispatchException(DispatchErrorTypeEnum.RATE_LIMITED, message);
    }

    public static DispatchException networkError(String message, Throwable cause) {
        return new DispatchException(DispatchErrorTypeEnum.NETWORK_ERROR, message, cause);
    }

    public static DispatchException platformError(String message) {
        return new DispatchException(DispatchErrorTypeEnum.PLATFORM_ERROR, message);
    }

    public static DispatchException notFound(String message) {
        return new DispatchException(DispatchErrorTypeEnum.ADAPTER_NOT_FOUND, message);
    }
}
