package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 客户端用户登录日志 t_login_log
 *
 * <p>只记录客户端聊天用户（t_user）的登录，与后台账号的 sys_logininfor 相互独立。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_login_log")
public class LoginLog extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 用户 UID（登录失败且账号不存在时为空）
     *  Jackson 从 getUId()/setUId() 推断出的名字是 uid，读写方法需显式声明为 uId */
    @Getter(onMethod_ = @JsonProperty("uId"))
    @Setter(onMethod_ = @JsonProperty("uId"))
    @TableField("u_id")
    private Long uId;

    /** 登录账号（手机号/邮箱） */
    private String account;

    /** 登录 IP */
    private String ip;

    /** 登录地点（离线为 IP 归属地解析结果） */
    @TableField("login_location")
    private String loginLocation;

    /** 设备类型 1-PC 2-平板 3-手机 */
    @TableField("device_type")
    private Integer deviceType;

    /** 设备唯一标识（客户端本地持久化的设备指纹） */
    @TableField("device_id")
    private String deviceId;

    /** 设备描述（设备类型 / User-Agent 简写） */
    @TableField("device_name")
    private String deviceName;

    /** 浏览器 */
    private String browser;

    /** 操作系统 */
    private String os;

    /** 登录状态 0-成功 1-失败 */
    private String status;

    /** 提示信息（成功原因或失败原因） */
    private String msg;

    /** 登录时间 */
    @TableField("login_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime loginTime;
}
