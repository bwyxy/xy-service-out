package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 系统访问记录表 sys_logininfor
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_logininfor")
public class SysLogininfor extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 访问ID
     * */
    @TableId(type = IdType.AUTO)
    private Long infoId;

    /**
     * 用户账号
     * */
    private String userName;

    /**
     * 登录IP地址
     * */
    private String ipaddr;

    /**
     * 登录地点
     * */
    private String loginLocation;

    /**
     * 浏览器类型
     * */
    private String browser;

    /**
     * 操作系统
     * */
    private String os;

    /**
     * 登录状态（0成功 1失败）
     * */
    private String status;

    /**
     * 提示消息
     * */
    private String msg;

    /**
     * 访问时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime loginTime;
}
