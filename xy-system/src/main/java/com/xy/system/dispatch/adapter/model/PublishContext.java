package com.xy.system.dispatch.adapter.model;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分发上下文
 * 由任务执行器组装，内容已完成平台格式适配与图片迁移
 * */
@Data
public class PublishContext {

    /** 文章 Id */
    private Long articleId;

    /** 标题（已按平台长度限制裁剪） */
    private String title;

    /** 摘要（已按平台长度限制裁剪） */
    private String summary;

    /** 正文（已按平台格式适配） */
    private String content;

    /** 封面图地址，可为空 */
    private String coverImage;

    /** 标签（已按平台数量与长度限制裁剪） */
    private List<String> tags = new ArrayList<>();

    /** 自己博客上的原文地址，可为空 */
    private String sourceUrl;

    /** 平台账号凭证（已解密） */
    private Map<String, String> credential = new HashMap<>();

    /** 平台发布默认规则 */
    private Map<String, Object> config = new HashMap<>();

    /** 平台配置 */
    private BlogDispatchPlatform platform;

    /** 请求超时（秒） */
    private int timeoutSeconds = 15;

    /** 最大重试次数 */
    private int maxRetry = 3;

    /**
     * 读取字符串型发布规则
     * */
    public String configString(String key) {
        Object value = config == null ? null : config.get(key);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * 读取布尔型发布规则，缺省返回 defaultValue
     * */
    public boolean configBoolean(String key, boolean defaultValue) {
        Object value = config == null ? null : config.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    /**
     * 读取整型发布规则，缺省返回 defaultValue
     * */
    public int configInt(String key, int defaultValue) {
        Object value = config == null ? null : config.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 读取凭证，缺失时返回空串
     * */
    public String credential(String key) {
        String value = credential == null ? null : credential.get(key);
        return value == null ? "" : value.trim();
    }
}
