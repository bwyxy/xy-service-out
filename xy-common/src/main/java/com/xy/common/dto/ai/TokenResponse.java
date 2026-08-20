package com.xy.common.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API 密钥换 token 响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    /**
     * 访问令牌
     */
    private String accessToken;

    /**
     * 令牌类型
     */
    private String tokenType;

    /**
     * 有效期（秒）
     */
    private Long expiresIn;
}
