package com.xy.common.vo.forum;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 论坛帖子 VO（列表/详情展示）
 */
@Data
public class ForumPostVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 帖子ID */
    private Long postId;

    /** 作者ID */
    private Long authorId;

    /** 作者昵称 */
    private String nickname;

    /** 作者头像 */
    private String avatar;

    /** 所属板块 */
    private Long boardId;

    /** 标题 */
    private String title;

    /** 摘要 */
    private String summary;

    /** 正文 */
    private String content;

    /** 内容类型：1-Markdown 2-富文本 */
    private Integer contentType;

    /** 封面图 */
    private String coverImage;

    /** 状态：0-草稿 1-审核中 2-已发布 3-拒绝 4-下架 */
    private Integer status;

    /** 可见性：1-公开 2-好友可见 3-私密 */
    private Integer visibility;

    /** 是否置顶 0-普通 1-置顶 */
    private Integer isTop;

    /** 是否精华 0-普通 1-精华 */
    private Integer isEssence;

    /** 浏览数 */
    private Long viewCount;

    /** 点赞数 */
    private Long likeCount;

    /** 评论数 */
    private Long commentCount;

    /** 收藏数 */
    private Long favoriteCount;

    /** 当前用户是否已点赞 */
    private Boolean liked;

    /** 当前用户是否已收藏 */
    private Boolean favorited;

    /** 发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime publishedTime;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
}
