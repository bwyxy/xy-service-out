package com.xy.common.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 论坛板块表 t_forum_board
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_board")
public class ForumBoard extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 板块ID
     */
    @TableId(value = "board_id", type = IdType.AUTO)
    private Long boardId;

    /**
     * 父板块ID，0-顶级
     */
    private Long parentId;

    /**
     * 板块名称
     */
    @NotNull(message = "版块名称不能 null")
    @NotBlank(message = "版块名称不能为空")
    private String name;

    /**
     * URL别名
     */
    private String slug;

    /**
     * 图标
     */
    private String icon;

    /**
     * 描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 状态 0-禁用 1-正常
     */
    private Integer status;

    /**
     * 帖子数
     */
    private Long postCount;

    /**
     * 关注数
     */
    private Long followCount;
}
