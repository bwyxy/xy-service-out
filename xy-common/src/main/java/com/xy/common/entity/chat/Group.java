package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 群组表 t_group
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_group")
public class Group extends BaseEntity implements Serializable {

    /**
     * 群组 Id
     * */
    @TableId(value = "group_id", type = IdType.AUTO)
    private Long groupId;

    /**
     * 群组名称
     * */
    private String name;

    /**
     * 群头像
     * */
    private String avatar;

    /**
     * 群主 Id
     * */
    private Long ownerUid;

    /**
     * 是否允许发言
     * */
    private Integer isBan;

    /**
     * 是否解散群组
     * */
    private Integer disband;

    /**
     * 群简介/公告摘要
     * */
    private String notice;

    /**
     * 最大成员数 - 200
     * */
    private Integer maxMember = 200;

    /**
     * 加群方式 0-邀请 1-扫码 2-自由加入
     * */
    private int joinType;

    /**
     * 邀请权限 0-仅管理员 1-所有成员
     * */
    private int invitePermission;

    /**
     * 发言权限 0-仅管理员 1-所有成员
     * */
    private int speakPermission;

    /**
     * 新成员是否可见历史消息 0-不可见 1-可见
     * */
    private int historyVisible;

    /**
     * 创建者
     * */
    private Long createBy;

    /**
     * 更新者
     * */
    private Long updateBy;
}
