package com.xy.common.entity.todo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户反馈表 t_todo_feedback
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("t_todo_feedback")
public class TodoFeedback implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 反馈 Id
     * */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户 Id t_user.u_id
     * */
    private Long userId;

    /**
     * 反馈内容
     * */
    private String content;

    /**
     * 图片 URL，逗号分隔
     * */
    private String images;

    /**
     * 用户自愿填写的联系方式
     * */
    private String contact;

    /**
     * 状态 0-待处理 1-已回复
     * */
    private Integer status;

    /**
     * 回复内容
     * */
    private String reply;

    /**
     * 创建时间
     * */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
