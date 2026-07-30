package com.xy.common.entity;

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
     * 创建者
     * */
    private Long createBy;

    /**
     * 更新者
     * */
    private Long updateBy;
}
