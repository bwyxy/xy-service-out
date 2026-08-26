package com.xy.common.entity.forum;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 帖子话题关联表 t_forum_post_topic
 */
@Data
@TableName("t_forum_post_topic")
public class ForumPostTopic implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 话题ID
     */
    private Long topicId;
}
