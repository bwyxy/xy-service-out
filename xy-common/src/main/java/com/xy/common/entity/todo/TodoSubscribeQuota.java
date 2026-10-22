package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 一次性订阅授权额度池表 t_todo_subscribe_quota（一次授权 = 一条消息）
 *
 * <p>主键为 user_id + template_id 复合主键，MyBatis-Plus 的 byId 方法不适用，请用条件构造器查询。</p>
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_subscribe_quota")
public class TodoSubscribeQuota implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 Id t_user.u_id
     * */
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

    /**
     * 更新时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
