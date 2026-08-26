package com.chat.fg.forum.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 论坛业务配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "forum")
public class ForumProperties {

    /**
     * 同一用户 5 分钟最多发帖数
     */
    private Integer postLimitPerUser = 3;

    /**
     * 发帖限流时间窗口（分钟）
     */
    private Integer postLimitWindowMinutes = 5;

    /**
     * 同一用户 1 分钟最多评论数
     */
    private Integer commentLimitPerUser = 10;

    /**
     * 评论限流时间窗口（分钟）
     */
    private Integer commentLimitWindowMinutes = 1;

    /**
     * 同一 IP 1 分钟最多搜索次数
     */
    private Integer searchLimitPerIp = 30;

    /**
     * 搜索限流时间窗口（分钟）
     */
    private Integer searchLimitWindowMinutes = 1;
}
