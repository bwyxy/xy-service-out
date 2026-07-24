package com.xy.app.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiChatRequest {

    private String message;

    private String conversationId;

    private List<Message> history;

    @Data
    public static class Message {
        private String role;
        private String content;
    }
}
