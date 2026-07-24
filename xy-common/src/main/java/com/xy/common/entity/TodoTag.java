package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 待办标签表 todo_tag
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("todo_tag")
public class TodoTag extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 10000000L;

    /**
     * 标签 Id
     * */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    /**
     * 关联用户 Id
     * */
    private Long userId;

    /**
     * 标签名称
     * */
    private String tagName;
}
