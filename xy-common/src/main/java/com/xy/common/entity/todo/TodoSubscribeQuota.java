package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 订阅消息授权额度表 t_todo_subscribe_quota（一次性订阅：授权一次 = 可下发一条）
 *
 * <p>业务主键是 (user_id, template_id) 复合键，而 MyBatis-Plus 只支持单主键，
 * 所以这里仅把 user_id 声明成逻辑主键，实际读写全部走 NotifyQuotaMapper 的手写 SQL。</p>
 * */
@Data
@TableName("t_todo_subscribe_quota")
public class TodoSubscribeQuota implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 Id t_user.u_id
     * */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    /**
     * 模板 Id
     * */
    private String templateId;

    /**
     * 剩余可下发条数
     * */
    private Integer remain;

    /**
     * 累计获得授权次数
     * */
    private Integer total;
}
