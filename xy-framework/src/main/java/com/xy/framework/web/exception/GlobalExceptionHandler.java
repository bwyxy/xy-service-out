package com.xy.framework.web.exception;

import com.xy.common.core.domain.R;
import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 *
 * <p>只把「有意暴露给用户」的异常信息回传前端（BusinessException/ServiceException
 * 以及参数校验类异常），兜底异常统一返回固定的通用提示，避免泄露 SQL、类名、
 * 堆栈摘要等内部实现细节。</p>
 * */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 兜底异常对外统一文案 */
    private static final String INTERNAL_ERROR_MESSAGE = "系统繁忙，请稍后重试";

    /**
     * 处理业务异常
     * */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理服务层异常
     *
     * <p>ServiceException 在本项目中承载的是可直接展示给用户的提示语，
     * 因此保留其文案，但按业务失败处理（HTTP 200 + 业务码），不再落到 500 兜底分支。</p>
     * */
    @ExceptionHandler(ServiceException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleServiceException(ServiceException e) {
        log.warn("服务异常: code={}, message={}", e.getCode(), e.getMessage());
        return R.error(e.getCode(), e.getMessage());
    }


    /**
     * 处理安全类异常
     *
     * <p>项目内 SecurityException 承载的是校验类提示语（密码不一致、验证码错误等），
     * 属于有意暴露给用户的信息，按业务失败处理返回其文案。</p>
     * */
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.OK)
    public R<Void> handleSecurityException(SecurityException e) {
        log.warn("安全异常: {}", e.getMessage());
        return R.error(ResultCode.BUSINESS_ERROR.getCode(), e.getMessage());
    }


    /**
     * 处理参数异校验常(@RequestBody)
     * */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleValidationException(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "参数校验失败";
        log.warn("参数校验异常：{}", message);
        return R.error(ResultCode.PARAM_VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 处理参数绑定异常（@RequestParam/@PathVariable）
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleBindException(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String message = fieldError != null
                ? fieldError.getField() + ": " + fieldError.getDefaultMessage()
                : "参数绑定失败";
        log.warn("参数绑定异常：{}", message);
        return R.error(ResultCode.PARAM_VALIDATION_ERROR.getCode(), message);
    }

    /**
     * 处理资源不存在
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Void> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("参数异常：{}", e.getMessage());
        return R.error(ResultCode.BAD_REQUEST.getCode(), e.getMessage());
    }

    /**
     * 处理其他运行时异常
     *
     * <p>此类异常属于未预期的内部错误，只记录完整堆栈，对外返回固定文案。</p>
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return R.error(ResultCode.INTERNAL_ERROR.getCode(), INTERNAL_ERROR_MESSAGE);
    }

    /**
     * 处理未知异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("未知异常", e);
        return R.error(ResultCode.INTERNAL_ERROR.getCode(), INTERNAL_ERROR_MESSAGE);
    }
}