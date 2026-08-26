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
 * 论坛点赞表 t_forum_like
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_like")
public class ForumLike extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 目标类型：1-帖子 2-评论
     */
    private Integer targetType;

    /**
     * 目标ID
     */
    private Long targetId;

    /**
     * 用户ID
     */
    private Long uId;

    /**
     * 状态：1-点赞 0-取消
     */
    private Integer status;
}
