package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 应用版本表 sys_app_version
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_app_version")
public class AppVersion extends BaseEntity {

    /**
     * 主键
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 应用标识 chat
     * */
    private String appKey;

    /**
     * 平台 web/windows/macos
     * */
    private String platform;

    /**
     * 展示版本号 1.2.0
     * */
    private String versionName;

    /**
     * 版本码 10200，比较大小只用它
     * */
    private Integer versionCode;

    /**
     * 安装包文件名
     * */
    private String fileName;

    /**
     * 安装包字节数
     * */
    private Long fileSize;

    /**
     * 安装包下载地址（七牛外链，web 平台可留空）
     * */
    private String downloadUrl;

    /**
     * 更新说明
     * */
    private String changelog;

    /**
     * 是否强制更新 0-可选 1-强制
     * */
    private String forceUpdate;

    /**
     * 状态 0-上架 1-下架
     * */
    private String status;

    /**
     * 创建者
     * */
    private String createBy;

    /**
     * 更新者
     * */
    private String updateBy;

    /**
     * 安装包大小展示文本（非数据库字段）
     * */
    @TableField(exist = false)
    private String fileSizeText;
}
