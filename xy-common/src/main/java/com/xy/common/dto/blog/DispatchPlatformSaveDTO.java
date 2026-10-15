package com.xy.common.dto.blog;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 分发平台配置保存 DTO
 * credentials 为明文凭证，服务端加密后再落库
 * */
@Data
public class DispatchPlatformSaveDTO {

    /** 主键Id，新增时为空 */
    private Long id;

    /** 平台编码 */
    private String platformCode;

    /** 平台名称 */
    private String platformName;

    /** 平台图标地址 */
    private String icon;

    /** 平台说明 */
    private String description;

    /** 是否启用 0-禁用 1-启用 */
    private Integer enabled;

    /** 账号凭证明文（仅传输，不落库；为空表示保持原凭证不变） */
    private Map<String, String> credentials;

    /** 发布默认规则（非敏感） */
    private Map<String, Object> config;

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

    /** 可使用该平台的角色 Id 列表，为空表示不限角色 */
    private List<Long> roleIds;

    /** 备注 */
    private String remark;
}
