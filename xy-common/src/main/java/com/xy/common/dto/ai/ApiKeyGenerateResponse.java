package com.xy.common.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * API 密钥生成响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKeyGenerateResponse {

    /**
     * API Key（可重复查看）
     */
    private String apiKey;

    /**
     * API Secret（仅创建时返回一次）
     */
    private String apiSecret;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * Token 有效期（秒）
     */
    private Integer tokenExpireSeconds;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;
}
