package com.chat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 模块 API 密钥
 */
@Data
@TableName("ai_api_key")
public class AiApiKey {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 访问密钥 Key
     */
    private String apiKey;

    /**
     * 访问密钥 Secret
     */
    private String apiSecret;

    /**
     * 是否启用 0-禁用 1-启用
     */
    private Integer enabled;

    /**
     * 密钥过期时间，NULL 表示永不过期
     */
    private LocalDateTime expireTime;

    /**
     * 签发的 token 有效期（秒）
     */
    private Integer tokenExpireSeconds;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
}
