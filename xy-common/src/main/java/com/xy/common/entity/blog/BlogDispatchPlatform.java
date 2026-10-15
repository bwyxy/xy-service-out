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
 * 分发平台配置表 blog_dispatch_platform
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_dispatch_platform")
public class BlogDispatchPlatform extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 平台编码 juejin/csdn/zhihu/wechat/jianshu/oschina */
    private String platformCode;

    /** 平台名称 */
    private String platformName;

    /** 平台图标地址 */
    private String icon;

    /** 平台说明 */
    private String description;

    /** 是否启用 0-禁用 1-启用 */
    private Integer enabled;

    /** 账号凭证密文（AES 加密后的 JSON，禁止明文落库） */
    private String credentialCipher;

    /** 发布默认规则（JSON，非敏感） */
    private String configJson;

    /** 标题最大长度 */
    private Integer titleMaxLength;

    /** 标签最大数量 */
    private Integer tagMaxCount;

    /** 摘要最大长度 */
    private Integer summaryMaxLength;

    /** 正文最大长度 */
    private Integer contentMaxLength;

    /** 每分钟最大请求数 */
    private Integer rateLimitPerMinute;

    /** 单次请求超时（秒） */
    private Integer requestTimeoutSeconds;

    /** 最大重试次数 */
    private Integer maxRetry;

    /** 排序 */
    private Integer sort;

    /** 最近一次连通性测试时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastTestTime;

    /** 最近一次连通性测试结果 */
    private String lastTestResult;

    /** 创建者 */
    private String createBy;

    /** 更新者 */
    private String updateBy;
}
