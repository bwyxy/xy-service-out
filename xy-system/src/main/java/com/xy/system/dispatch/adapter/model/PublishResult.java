package com.xy.system.dispatch.adapter.model;

import com.xy.common.enums.DispatchErrorTypeEnum;
import lombok.Data;

/**
 * 平台发布结果
 * */
@Data
public class PublishResult {

    /** 是否发布成功 */
    private boolean success;

    /** 平台侧文章 Id */
    private String platformArticleId;

    /** 平台侧文章访问链接 */
    private String resultUrl;

    /** 失败类型 */
    private DispatchErrorTypeEnum errorType = DispatchErrorTypeEnum.NONE;

    /** 失败原因 */
    private String errorMsg;

    public static PublishResult ok(String platformArticleId, String resultUrl) {
        PublishResult result = new PublishResult();
        result.setSuccess(true);
        result.setPlatformArticleId(platformArticleId);
        result.setResultUrl(resultUrl);
        result.setErrorType(DispatchErrorTypeEnum.NONE);
        return result;
    }

    public static PublishResult fail(DispatchErrorTypeEnum errorType, String errorMsg) {
        PublishResult result = new PublishResult();
        result.setSuccess(false);
        result.setErrorType(errorType == null ? DispatchErrorTypeEnum.UNKNOWN : errorType);
        result.setErrorMsg(errorMsg);
        return result;
    }
}
