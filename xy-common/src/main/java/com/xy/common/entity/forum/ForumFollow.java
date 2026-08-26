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
 * 论坛关注表 t_forum_follow
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_follow")
public class ForumFollow extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关注者u_id
     */
    private Long uId;

    /**
     * 关注类型：1-用户 2-板块 3-话题
     */
    private Integer followType;

    /**
     * 被关注对象ID
     */
    private Long targetId;
}
