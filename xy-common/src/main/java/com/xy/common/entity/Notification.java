package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 通知表 t_notification
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_notification")
public class Notification extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 接收通知的用户ID
     * */
    private Long uId;

    /**
     * 通知类型 1-好友请求 2-好友同意 3-系统通知
     * */
    private Integer type;

    /**
     * 通知标题
     * */
    private String title;

    /**
     * 通知内容
     * */
    private String content;

    /**
     * 关联业务记录ID
     * */
    private Long relatedId;

    /**
     * 是否已读 0-未读 1-已读
     * */
    private Integer isRead;

    /**
     * 扩展信息
     * */
    @TableField("extra")
    private String extra;
}
