package com.xy.common.vo.blog;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 分发平台配置 VO（脱敏，不返回任何凭证明文或密文）
 * */
@Data
public class DispatchPlatformVO {

    private Long id;

    private String platformCode;

    private String platformName;

    private String icon;

    private String description;

    private Integer enabled;

    /** 是否已配置凭证 */
    private Boolean credentialConfigured;

    /** 已配置的凭证字段名列表（仅键名，便于前端回显"已配置"状态） */
    private List<String> credentialKeys;

    /** 适配器要求的凭证字段名列表（用于渲染表单） */
    private List<String> requiredCredentialKeys;

    /** 是否存在可用的适配器实现 */
    private Boolean adapterSupported;

    /** 发布默认规则 */
    private Map<String, Object> config;

    private Integer titleMaxLength;

    private Integer tagMaxCount;

    private Integer summaryMaxLength;

    private Integer contentMaxLength;

    private Integer rateLimitPerMinute;

    private Integer requestTimeoutSeconds;

    private Integer maxRetry;

    private Integer sort;

    /** 可使用该平台的角色 Id 列表，为空表示不限角色 */
    private List<Long> roleIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime lastTestTime;

    private String lastTestResult;

    private String remark;
}
