package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * API 密钥换 token 请求
 */
@Data
public class TokenRequest {

    /**
     * API Key
     */
    @NotBlank(message = "apiKey 不能为空")
    private String apiKey;

    /**
     * API Secret
     */
    @NotBlank(message = "apiSecret 不能为空")
    private String apiSecret;
}
