package com.xy.common.entity.forum;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 论坛帖子表 t_forum_post
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_post")
public class ForumPost extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 帖子ID
     */
    @TableId(value = "post_id", type = IdType.AUTO)
    private Long postId;

    /**
     * 作者u_id
     */
    private Long authorId;

    /**
     * 所属板块
     */
    private Long boardId;

    /**
     * 标题
     */
    private String title;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 正文
     */
    private String content;

    /**
     * 内容类型：1-Markdown 2-富文本
     */
    private Integer contentType;

    /**
     * 封面图
     */
    private String coverImage;

    /**
     * 状态：0-草稿 1-审核中 2-已发布 3-拒绝 4-下架
     */
    private Integer status;

    /**
     * 可见性：1-公开 2-好友可见 3-私密
     */
    private Integer visibility;

    /**
     * 是否置顶 0-普通 1-置顶
     */
    private Integer isTop;

    /**
     * 是否精华 0-普通 1-精华
     */
    private Integer isEssence;

    /**
     * 浏览数
     */
    private Long viewCount;

    /**
     * 点赞数
     */
    private Long likeCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 收藏数
     */
    private Long favoriteCount;

    /**
     * 分享数
     */
    private Long shareCount;

    /**
     * 分表路由键
     */
    private Long routeKey;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedTime;
}
