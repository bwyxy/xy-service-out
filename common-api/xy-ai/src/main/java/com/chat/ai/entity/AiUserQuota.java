package com.chat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 用户调用配额
 */
@Data
@TableName("ai_user_quota")
public class AiUserQuota {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userType;

    private Integer dailyLimit;

    private Integer dailyUsed;

    private Long totalUsed;

    private LocalDateTime resetTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
