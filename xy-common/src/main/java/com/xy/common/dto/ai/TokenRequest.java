package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 密钥换 token 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
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
