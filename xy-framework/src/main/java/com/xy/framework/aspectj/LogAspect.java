package com.xy.framework.aspectj;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.xy.common.annotation.Log;
import com.xy.common.core.domain.model.LoginUser;
import com.xy.common.entity.SysOperLog;
import com.xy.common.ip.IpUtils;
import com.xy.common.utils.SecurityUtils;
import com.xy.common.utils.StringUtils;
import com.xy.system.service.ISysOperLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志记录处理
 * */
@Aspect
@Component
@Slf4j
public class LogAspect {

    /**
     * 排除敏感字段
     * */
    private static final String[] SENSITIVE_FIELDS = {"password", "oldPassword", "newPassword", "confirmPassword"};

    private static final NamedThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<>("Cost Time");

    @Autowired
    private ISysOperLogService operLogService;

    /**
     * 处理前执行
     * */
    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog) {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行
     * */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult) {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作
     * */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e) {
        handleLog(joinPoint, controllerLog, e, null);
    }

    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult) {
        try {
            SysOperLog operLog = new SysOperLog();
            operLog.setStatus(0);
            operLog.setOperTime(LocalDateTime.now());

            Long startTime = TIME_THREADLOCAL.get();
            if (startTime != null) {
                operLog.setCostTime(System.currentTimeMillis() - startTime);
            }

            HttpServletRequest request = getRequest();
            if (request != null) {
                operLog.setOperUrl(request.getRequestURI());
                operLog.setRequestMethod(request.getMethod());
                operLog.setOperIp(IpUtils.getIpAddr(request));
            }

            operLog.setTitle(controllerLog.title());
            operLog.setBusinessType(controllerLog.businessType().ordinal());
            operLog.setMethod(joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName() + "()");

            if (controllerLog.isSaveRequestData()) {
                operLog.setOperParam(argsArrayToString(joinPoint.getArgs()));
            }
            if (controllerLog.isSaveResponseData() && StringUtils.isNotNull(jsonResult)) {
                operLog.setJsonResult(JSON.toJSONString(jsonResult));
            }

            LoginUser loginUser = null;
            try {
                loginUser = SecurityUtils.getLoginUser();
            } catch (Exception ex) {
                // 未登录场景忽略
            }
            if (loginUser != null) {
                operLog.setOperName(loginUser.getUsername());
            }

            if (e != null) {
                operLog.setStatus(1);
                operLog.setErrorMsg(StringUtils.substring(e.getMessage(), 0, 2000));
            }

            operLogService.save(operLog);
        } catch (Exception ex) {
            log.error("操作日志记录异常：", ex);
        } finally {
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 参数拼装
     * */
    private String argsArrayToString(Object[] paramsArray) {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0) {
            for (Object o : paramsArray) {
                if (StringUtils.isNotNull(o) && !isFilterObject(o)) {
                    try {
                        Object param = desensitize(o);
                        String jsonObj = JSON.toJSONString(param);
                        params.append(jsonObj).append(" ");
                    } catch (Exception e) {
                        log.error("日志参数序列化异常", e);
                    }
                }
            }
        }
        return params.toString().trim();
    }

    /**
     * 对参数中的敏感字段进行脱敏
     * */
    private Object desensitize(Object param) {
        if (param == null) {
            return null;
        }
        try {
            String json = JSON.toJSONString(param);
            JSONObject jsonObject = JSON.parseObject(json);
            for (String field : SENSITIVE_FIELDS) {
                if (jsonObject.containsKey(field)) {
                    jsonObject.put(field, "******");
                }
            }
            return jsonObject;
        } catch (Exception e) {
            return param;
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            return attributes.getRequest();
        }
        return null;
    }

    /**
     * 判断是否需要过滤的对象。
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o) {
        Class<?> clazz = o.getClass();
        if (clazz.isArray()) {
            return clazz.getComponentType().isAssignableFrom(HttpServletRequest.class);
        }
        return o instanceof HttpServletRequest || o instanceof HttpServletResponse;
    }
}
