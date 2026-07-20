package com.todo.ai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiController {

    private final ChatClient chatClient;


    public AiController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/ai/chat")
    public ResponseEntity<?> chat(@RequestParam String message) {
        try {
            String content = this.chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            return ResponseEntity.ok(content);
        } catch (NonTransientAiException e) {
            if (e.getMessage() != null && e.getMessage().contains("Insufficient Balance")) {
                return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                        .body("AI 服务账户余额不足，请联系管理员充值。");
            }
            // 其他异常处理...
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI 服务暂不可用");
        }
    }
}
