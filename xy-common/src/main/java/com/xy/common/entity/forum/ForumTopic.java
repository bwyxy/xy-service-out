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
 * 论坛话题表 t_forum_topic
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_topic")
public class ForumTopic extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 话题ID
     */
    @TableId(value = "topic_id", type = IdType.AUTO)
    private Long topicId;

    /**
     * 话题名
     */
    private String name;

    /**
     * URL别名
     */
    private String slug;

    /**
     * 描述
     */
    private String description;

    /**
     * 封面
     */
    private String coverImage;

    /**
     * 帖子数
     */
    private Long postCount;

    /**
     * 关注数
     */
    private Long followCount;

    /**
     * 状态：0-禁用 1-正常
     */
    private Integer status;
}
