package com.chat.fg.client;

import com.chat.fg.pojo.dto.AiChatRequest;
import com.chat.fg.pojo.dto.AiChatResponse;
import com.xy.common.core.domain.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * AI 对话服务 Feign 客户端
 */
@FeignClient(name = "aiChatClient", url = "${ai.service.url:http://localhost:9070}")
public interface AiChatClient {

    /**
     * 同步对话
     *
     * @param token     xy-ai access token
     * @param request   AI 对话请求
     */
    @PostMapping("/ai/chat")
    R<AiChatResponse> chat(@RequestHeader("accessToken") String token, @RequestBody AiChatRequest request);
}
