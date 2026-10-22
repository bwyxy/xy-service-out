package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户维度同步序号发号器表 t_todo_user_seq
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_user_seq")
public class TodoUserSeq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 Id t_user.u_id
     * */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 当前最大序号
     * */
    private Long seq;
}
