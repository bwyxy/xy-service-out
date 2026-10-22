package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 客户端待办清单表 t_todo_tag（前端叫「清单」）
 *
 * <p>注意：与管理端旧表 todo_tag 的实体 {@link com.xy.common.entity.TodoTag} 无关。</p>
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_tag")
public class TodoTag extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 清单 Id
     * */
    @TableId(value = "tag_id", type = IdType.AUTO)
    private Long tagId;

    /**
     * 所属用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 清单名称
     * */
    private String tagName;

    /**
     * 图标名（对应前端 y-icon 的 name）
     * */
    private String icon;

    /**
     * 主题色
     * */
    private String color;

    /**
     * 排序
     * */
    private Integer sortNo;

    /**
     * 是否默认清单
     * */
    private Integer isDefault;

    /**
     * 是否归档
     * */
    private Integer archived;

    /**
     * 创建者
     * */
    private String createBy;

    /**
     * 更新者
     * */
    private String updateBy;
}
