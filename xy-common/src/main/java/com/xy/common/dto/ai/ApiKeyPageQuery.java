package com.xy.common.dto.ai;

import lombok.Data;

/**
 * API 密钥分页查询条件
 */
@Data
public class ApiKeyPageQuery {

    /**
     * 应用名称（模糊查询）
     */
    private String appName;

    /**
     * API Key（精确查询）
     */
    private String apiKey;

    /**
     * 是否启用 0-禁用 1-启用
     */
    private Integer enabled;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
