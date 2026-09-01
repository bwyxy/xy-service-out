package com.chat.fg.pojo.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创作中心统计数据 VO
 */
@Data
public class CreatorStatsVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 粉丝数
     */
    private Long followerCount = 0L;

    /**
     * 关注数
     */
    private Long followingCount = 0L;

    /**
     * 获赞数
     */
    private Long likeCount = 0L;

    /**
     * 获收藏数
     */
    private Long favoriteCount = 0L;

    /**
     * 获评论数
     */
    private Long commentCount = 0L;
}
