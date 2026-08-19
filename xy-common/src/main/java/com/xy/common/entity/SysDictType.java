package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典类型表 sys_dict_type
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典主键
     * */
    @TableId(type = IdType.AUTO)
    private Long dictId;

    /**
     * 字典名称
     * */
    private String dictName;

    /**
     * 字典类型
     * */
    private String dictType;

    /**
     * 状态（0正常 1停用）
     * */
    private String status;

    /**
     * 创建者
     * */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /**
     * 更新者
     * */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;
}
