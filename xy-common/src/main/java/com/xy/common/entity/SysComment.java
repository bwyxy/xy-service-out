package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 评论表 sys_comment
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("sys_comment")
public class SysComment extends BaseEntity implements Serializable {

    /**
     * 评论 Id
     * */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 Id
     * */
    private Long articleId;

    /**
     * 用户 Id
     * */
    private Long userId;
    /**
     * 匿名头像 emoji
     * */
    private String avatarEmoji;
    /**
     * 匿名名称
     * */
    private String nickname;
    /**
     * 匿名邮箱
     * */
    private String email;
    /**
     * 评论 ip
     * */
    private String commentIp;
    /**
     * IP 地址
     * */
    private String ipAddress;
    /**
     * 设备 Id
     * */
    private String deviceId;
    /**
     * 设备信息
     * */
    private String deviceInfo;
    /**
     * 评论类型
     * article-文章 message-留言
     * */
    private String commentType;
    /**
     * 内容
     * */
    private String content;
    /**
     * 父评论 Id
     * */
    private Long parentId;
    /**
     * 根评论 Id
     * */
    private Long rootId;
    /**
     * 层级
     * */
    private Integer level;
    /**
     * 点赞数量
     * */
    private Integer likeCount;
    /**
     * 状态
     * 0-待审核 1-显示 2-删除
     * */
    private Integer status;

    // 非数据库字段：用于前端展示的子评论列表
    @TableField(exist = false)
    private List<SysComment> children;
}
