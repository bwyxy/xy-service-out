package com.todo.app.controller;

import com.todo.app.pojo.dto.AiChatRequest;
import com.todo.app.pojo.vo.AiChatResponse;
import com.todo.app.service.IAiAgentService;
import com.todo.common.annotation.Anonymous;
import com.todo.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiAgentController {

    private final IAiAgentService aiAgentService;

    @Anonymous
    @PostMapping("/chat")
    public R<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        AiChatResponse response = aiAgentService.chat(request);
        return R.ok(response);
    }

    @Anonymous
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody AiChatRequest request) {
        return aiAgentService.chatStream(request);
    }
}
