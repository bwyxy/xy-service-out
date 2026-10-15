package com.xy.common.vo.blog;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分发任务 VO
 * */
@Data
public class DispatchTaskVO {

    private Long id;

    private String batchNo;

    private Long articleId;

    private String articleTitle;

    private String platformCode;

    private String platformName;

    /** 状态 0-待分发 1-分发中 2-分发成功 3-分发失败 */
    private Integer status;

    /** 状态中文描述 */
    private String statusDesc;

    private Integer retryCount;

    private Integer maxRetry;

    /** 错误类型编码 */
    private String errorType;

    /** 错误类型中文描述 */
    private String errorDesc;

    /** 失败原因明细 */
    private String errorMsg;

    /** 是否需要人工介入处理（如重新配置凭证、修改内容） */
    private Boolean needManualFix;

    /** 是否可自动重试 */
    private Boolean retryable;

    /** 平台侧文章 Id */
    private String platformArticleId;

    /** 平台侧文章访问链接 */
    private String resultUrl;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finishTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
