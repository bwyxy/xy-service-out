package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 增量同步游标表 t_todo_sync_cursor（一用户一设备一游标）
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_sync_cursor")
public class TodoSyncCursor implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 设备 Id
     * */
    private String deviceId;

    /**
     * 已同步到的服务端序号
     * */
    private Long lastSeq;

    /**
     * 上次同步时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastSyncTime;
}
