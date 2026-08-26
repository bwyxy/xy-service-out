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
 * 论坛用户扩展资料表 t_forum_user_profile
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_forum_user_profile")
public class ForumUserProfile extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long uId;

    /**
     * 等级
     */
    private Integer level;

    /**
     * 签名
     */
    private String signature;

    /**
     * 发帖数
     */
    private Long postCount;

    /**
     * 评论数
     */
    private Long commentCount;

    /**
     * 获赞数
     */
    private Long likeReceivedCount;

    /**
     * 积分
     */
    private Long score;
}
