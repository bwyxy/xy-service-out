package com.chat.ai.controller;

import com.chat.ai.dto.ChatRequest;
import com.chat.ai.dto.ChatResponse;
import com.chat.ai.provider.core.AiStreamChunk;
import com.chat.ai.service.AiChatService;
import com.xy.common.annotation.Anonymous;
import com.xy.common.core.domain.R;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 对话接口
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiChatService chatService;

    public AiController(AiChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 同步对话
     */
    @PostMapping("/chat")
    public R<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return R.ok(chatService.chat(request));
    }

    /**
     * 流式对话（SSE）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<AiStreamChunk> streamChat(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(request).onErrorResume(error -> {
            log.warn("流式对话异常", error);
            return Flux.just(AiStreamChunk.builder()
                    .errorMsg(error.getMessage())
                    .done(true)
                    .build());
        });
    }
}
