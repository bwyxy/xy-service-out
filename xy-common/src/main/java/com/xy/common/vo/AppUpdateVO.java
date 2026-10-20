package com.xy.common.vo;

import lombok.Data;

/**
 * 客户端检查更新响应
 * */
@Data
public class AppUpdateVO {

    /**
     * 是否有新版本
     * */
    private Boolean hasUpdate = false;

    /**
     * 最新版本号（展示用）
     * */
    private String latestVersion;

    /**
     * 最新版本码
     * */
    private Integer latestVersionCode;

    /**
     * 是否强制更新
     * */
    private Boolean forceUpdate = false;

    /**
     * 安装包下载地址（web 平台为空，前端直接刷新页面）
     * */
    private String downloadUrl;

    /**
     * 安装包字节数
     * */
    private Long fileSize;

    /**
     * 更新说明
     * */
    private String changelog;
}
