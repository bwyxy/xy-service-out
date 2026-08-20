package com.chat.ai.dto;

import lombok.Data;

/**
 * 会话查询条件
 */
@Data
public class ConversationQuery {

    private Long userId;

    private String userType;

    private String keyword;

    private Integer status;
}
