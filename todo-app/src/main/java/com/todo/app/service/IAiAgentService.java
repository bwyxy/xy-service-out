package com.todo.app.service;

import com.todo.app.pojo.dto.AiChatRequest;
import com.todo.app.pojo.vo.AiChatResponse;
import reactor.core.publisher.Flux;

public interface IAiAgentService {

    AiChatResponse chat(AiChatRequest request);

    Flux<String> chatStream(AiChatRequest request);
}
