package com.xy.app.service.support;

import com.xy.common.enums.ResultCode;
import com.xy.common.exception.BusinessException;
import com.xy.common.utils.ContentCheckUtils;
import com.xy.common.utils.StringUtils;

import java.time.LocalDateTime;

/**
 * 待办任务入参校验（无状态，只管校验与兜底，不碰数据库）
 * */
public final class TodoTaskValidator {

    private TodoTaskValidator() {
    }

    /**
     * 校验任务标题，返回去掉首尾空格后的标题
     * */
    public static String validateTitle(String title) {
        String trimmed = title == null ? null : title.trim();
        ContentCheckUtils.check(trimmed, 1, 255);

        return trimmed;
    }

    /**
     * 校验任务描述
     * */
    public static void validateDescription(String description) {
        if (StringUtils.hasText(description)) {
            ContentCheckUtils.check(description, 1, 5000);
        }
    }

    /**
     * 校验紧急程度，返回兜底后的值
     * */
    public static int validateUrgencyLevel(Integer urgencyLevel) {
        int value = urgencyLevel == null ? 1 : urgencyLevel;
        if (value < 1 || value > 4) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "紧急程度必须在 1~4 之间");
        }

        return value;
    }

    /**
     * 校验优先级，返回兜底后的值
     * */
    public static int validatePriority(Integer priority) {
        int value = priority == null ? 0 : priority;
        if (value < 0 || value > 2) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "优先级必须在 0~2 之间");
        }

        return value;
    }

    /**
     * 校验提前提醒分钟数，返回兜底后的值
     * */
    public static int validateRemindBefore(Integer remindBefore, LocalDateTime dueDate) {
        int value = remindBefore == null ? 0 : remindBefore;
        if (value < 0) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "提前提醒分钟数不能为负数");
        }
        if (value > 0 && dueDate == null) {
            throw new BusinessException(ResultCode.PARAM_VALIDATION_ERROR, "设置了提醒就必须填写截止时间");
        }

        return value;
    }

    /**
     * 页码/每页条数兜底：为空或非正数时取默认值
     * */
    public static long normalizePositive(Integer value, long defaultValue) {
        return value == null || value <= 0 ? defaultValue : value;
    }
}
