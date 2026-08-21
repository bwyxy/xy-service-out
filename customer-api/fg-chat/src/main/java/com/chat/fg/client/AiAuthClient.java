package com.chat.fg.client;

import com.xy.common.core.domain.R;
import com.xy.common.dto.ai.TokenRequest;
import com.xy.common.dto.ai.TokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * xy-ai 认证服务 Feign 客户端
 */
@FeignClient(name = "aiAuthClient", url = "${ai.service.url:http://localhost:9070}")
public interface AiAuthClient {

    /**
     * 使用 API Key + Secret 换取访问令牌
     */
    @PostMapping("/ai/auth/token")
    R<TokenResponse> getToken(@RequestBody TokenRequest request);
}
