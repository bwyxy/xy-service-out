package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * API 密钥生成请求
 */
@Data
public class ApiKeyGenerateRequest {

    /**
     * 应用名称
     */
    @NotBlank(message = "应用名称不能为空")
    private String appName;

    /**
     * Token 有效期（秒），默认 3600
     */
    @NotNull(message = "token 有效期不能为空")
    private Integer tokenExpireSeconds;
}
