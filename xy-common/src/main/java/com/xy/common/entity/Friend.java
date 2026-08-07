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
 * 好友表 t_friend
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_friend")
public class Friend extends BaseEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     * */
    private Long uId;

    /**
     * 好友用户ID
     * */
    private Long friendId;

    /**
     * 状态 0-正常 1-拉黑
     * */
    private String status;
}
