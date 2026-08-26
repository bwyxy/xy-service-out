package com.chat.fg.forum.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子展示 VO
 */
@Data
public class PostVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long postId;
    private Long authorId;
    private String authorNickname;
    private String authorAvatar;
    private Long boardId;
    private String boardName;
    private String title;
    private String summary;
    private String content;
    private Integer contentType;
    private String coverImage;
    private Integer status;
    private Integer visibility;
    private Integer isTop;
    private Integer isEssence;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long favoriteCount;
    private LocalDateTime publishedTime;
    private LocalDateTime createTime;
    private List<String> topicNames;
}
