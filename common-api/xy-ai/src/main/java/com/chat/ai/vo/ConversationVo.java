package com.chat.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 会话视图对象
 */
@Data
public class ConversationVo {

    private Long id;

    private Long userId;

    private String userType;

    private String title;

    private String providerCode;

    private String model;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
