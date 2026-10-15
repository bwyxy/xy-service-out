package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 分发任务表 blog_dispatch_task
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_dispatch_task")
public class BlogDispatchTask extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 批次号（批量分发时同批共用） */
    private String batchNo;

    /** 文章 Id */
    private Long articleId;

    /** 文章标题快照 */
    private String articleTitle;

    /** 目标平台编码 */
    private String platformCode;

    /** 目标平台名称快照 */
    private String platformName;

    /**
     * 状态
     * 0-待分发 1-分发中 2-分发成功 3-分发失败
     * */
    private Integer status;

    /** 已重试次数 */
    private Integer retryCount;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 错误类型编码 */
    private String errorType;

    /** 失败原因 */
    private String errorMsg;

    /** 平台侧文章 Id */
    private String platformArticleId;

    /** 平台侧文章访问链接 */
    private String resultUrl;

    /** 执行耗时（毫秒） */
    private Long durationMs;

    /** 开始执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime startTime;

    /** 执行结束时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime finishTime;

    /** 触发分发的用户 Id */
    private Long operatorId;
}
