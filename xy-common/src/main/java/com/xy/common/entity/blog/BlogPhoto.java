package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 照片表 blog_photo
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_photo")
public class BlogPhoto extends BaseEntity {

    @TableId
    private Long id;

    /**
     * 分类Id 分类类型为album的
     * */
    private Long categoryId;

    /**
     * 名称
     * */
    private String name;

    /**
     * 照片链接
     * */
    private String photoUrl;

    /**
     * 照片状态
     * 0-私密 1-公开
     * */
    private int status;

    /**
     * 描述
     * */
    private String description;

    /**
     * 排序
     * */
    private int sort;

    /**
     * 拍摄时间
     * */
    private LocalDateTime publishedTime;
}
