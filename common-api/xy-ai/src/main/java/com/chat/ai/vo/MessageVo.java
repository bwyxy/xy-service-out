package com.chat.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息视图对象
 */
@Data
public class MessageVo {

    private Long id;

    private Long conversationId;

    private String role;

    private String content;

    private String model;

    private Integer status;

    private LocalDateTime createTime;
}
