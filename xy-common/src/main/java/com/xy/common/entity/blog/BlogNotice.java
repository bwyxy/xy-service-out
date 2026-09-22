package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 公告表 blog_notice
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_notice")
public class BlogNotice extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容 */
    private String content;

    /**
     * 类型
     * 1-普通 2-重要 3-紧急
     * */
    private Integer type;

    /**
     * 状态
     * 1-发布 0-草稿 2-下线
     * */
    private Integer status;

    /**
     * 是否置顶
     * */
    private int isTop;

    /** 排序 */
    private int sort;

    /** 跳转链接 */
    private String linkUrl;

    /**
     * 生效时间 为空立即生效
     * */
    private LocalDateTime startTime;

    /**
     * 失效时间 为空永久有效
     * */
    private LocalDateTime endTime;
}
