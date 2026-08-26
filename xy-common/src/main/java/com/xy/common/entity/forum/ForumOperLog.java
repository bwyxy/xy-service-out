package com.xy.common.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 论坛操作日志表 t_forum_oper_log
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_oper_log")
public class ForumOperLog extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 操作人ID
     */
    private Long uId;

    /**
     * 操作类型
     */
    private String operType;

    /**
     * 对象类型
     */
    private String targetType;

    /**
     * 对象ID
     */
    private Long targetId;

    /**
     * 操作详情
     */
    private String detail;

    /**
     * IP地址
     */
    private String ip;
}
