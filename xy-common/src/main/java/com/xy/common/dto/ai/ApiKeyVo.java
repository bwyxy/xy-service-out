package com.xy.common.dto.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * API 密钥列表展示对象
 */
@Data
public class ApiKeyVo {

    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 是否启用 0-禁用 1-启用
     */
    private Integer enabled;

    /**
     * 密钥过期时间
     */
    private LocalDateTime expireTime;

    /**
     * Token 有效期（秒）
     */
    private Integer tokenExpireSeconds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
