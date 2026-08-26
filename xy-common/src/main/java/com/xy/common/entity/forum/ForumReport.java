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
 * 论坛举报表 t_forum_report
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_report")
public class ForumReport extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 举报ID
     */
    @TableId(value = "report_id", type = IdType.AUTO)
    private Long reportId;

    /**
     * 举报人u_id
     */
    private Long reporterId;

    /**
     * 目标类型：1-帖子 2-评论 3-用户
     */
    private Integer targetType;

    /**
     * 被举报对象ID
     */
    private Long targetId;

    /**
     * 举报原因
     */
    private String reason;

    /**
     * 状态：0-待处理 1-已处理
     */
    private Integer status;

    /**
     * 处理人ID
     */
    private Long handlerId;

    /**
     * 处理结果
     */
    private String result;
}
