package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 访问日志表 blog_event_log  */
@Data
@TableName("blog_event_log")
public class BlogEventLog {

    @TableId
    private Long id;

    /**
     * 事件类型: page_view/click/submit等
     * */
    private String eventType;

    /**
     * 界面路径
     * */
    private String path;

    /**
     * 来源页
     * */
    private String referrer;

    /**
     * 界面标题
     * */
    private String title;

    /**
     * 匿名用户 Id
     * */
    private String visitorId;

    /**
     * 会话 Id
     * */
    private String sessionId;

    /**
     * 登录用户 Id
     * */
    private Long loginUserId;

    /**
     * 客户端 IP，兼容 IPv6
     * */
    private String ip;

    /**
     * User-Agent
     * */
    private String ua;

    /**
     * 设备类型: PC/Mobile
     * */
    private String device;

    /**
     * 操作系统
     * */
    private String os;

    /**
     * 浏览器
     * */
    private String browser;

    /**
     * 自定义属性
     * */
    private String props;

    /**
     * 事件发生时间，毫秒精度
     * */
    private LocalDateTime createdAt;
}
