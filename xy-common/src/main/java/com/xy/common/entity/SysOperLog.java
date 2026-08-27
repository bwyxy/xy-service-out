package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 操作日志记录表 sys_oper_log
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_oper_log")
public class SysOperLog extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 日志主键
     * */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作模块
     * */
    private String title;

    /**
     * 业务类型（0其它 1新增 2修改 3删除 4授权 5导出 6导入 7强退 8生成代码 9清空数据）
     * */
    private Integer businessType;

    /**
     * 业务类型数组
     * */
    @TableField(exist = false)
    private Integer[] businessTypes;

    /**
     * 请求方法
     * */
    private String method;

    /**
     * 请求方式
     * */
    private String requestMethod;

    /**
     * 操作类别（0其它 1后台用户 2手机端用户）
     * */
    private Integer operatorType;

    /**
     * 操作人员
     * */
    private String operName;

    /**
     * 部门名称
     * */
    private String deptName;

    /**
     * 请求url
     * */
    private String operUrl;

    /**
     * 操作地址
     * */
    private String operIp;

    /**
     * 操作地点
     * */
    private String operLocation;

    /**
     * 请求参数
     * */
    private String operParam;

    /**
     * 返回参数
     * */
    private String jsonResult;

    /**
     * 操作状态（0正常 1异常）
     * */
    private Integer status;

    /**
     * 错误消息
     * */
    private String errorMsg;

    /**
     * 操作时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime operTime;

    /**
     * 消耗时间
     * */
    private Long costTime;
}
