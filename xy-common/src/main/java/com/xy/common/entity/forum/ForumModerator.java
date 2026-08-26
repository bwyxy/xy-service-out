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
 * 版主表 t_forum_moderator
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_moderator")
public class ForumModerator extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 板块ID
     */
    private Long boardId;

    /**
     * 用户ID
     */
    private Long uId;

    /**
     * 角色：1-版主 2-超级版主
     */
    private Integer role;
}
