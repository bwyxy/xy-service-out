package com.xy.common.entity.chat;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群公告表 t_group_announcement
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_group_announcement")
public class GroupAnnouncement extends BaseEntity {

    /**
     * id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联群组ID
     * */
    private Long groupId;

    /**
     * 标题
     * */
    private String title;

    /**
     * 内容
     * */
    private String content;

    /**
     * 发布人ID
     * */
    private Long publisherUid;

    /**
     * 是否置顶
     * */
    private int isPinned;
}
