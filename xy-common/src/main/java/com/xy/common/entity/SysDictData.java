package com.xy.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 字典数据表 sys_dict_data
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典编码
     * */
    @TableId(type = IdType.AUTO)
    private Long dictCode;

    /**
     * 字典排序
     * */
    private Integer dictSort;

    /**
     * 字典标签
     * */
    private String dictLabel;

    /**
     * 字典键值
     * */
    private String dictValue;

    /**
     * 字典类型
     * */
    private String dictType;

    /**
     * 样式属性（其他样式扩展）
     * */
    private String cssClass;

    /**
     * 表格回显样式
     * */
    private String listClass;

    /**
     * 是否默认（Y是 N否）
     * */
    private String isDefault;

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
