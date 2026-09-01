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
 * 论坛评论表 t_forum_comment
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_comment")
public class ForumComment extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "comment_id", type = IdType.AUTO)
    private Long commentId;

    /**
     * 帖子ID
     */
    private Long postId;

    /**
     * 评论者u_id
     */
    private Long authorId;

    /**
     * 父评论ID，0-根评论
     */
    private Long parentId;

    /**
     * 根评论ID
     */
    private Long rootId;

    /**
     * 被回复者u_id
     */
    private Long replyToUid;

    /**
     * 内容
     */
    private String content;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 状态：0-待审核 1-显示 2-删除
     */
    private Integer status;

    /**
     * ip 地址
     * */
    private String ip;

    /**
     * ip 所属地
     * */
    private String ipAddr;
}
