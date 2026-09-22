package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工具表 blog_tool
 * */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("blog_tool")
public class BlogTool extends BaseEntity {

    @TableId
    private Long id;

    /**
     * 分类Id 分类类型为tool 的
     * */
    private Long categoryId;

    /** 工具名称 */
    private String name;

    /** 工具地址 */
    private String url;

    /** icon */
    private String icon;

    /** 描述 */
    private String description;

    /**
     * 平台
     * */
    private String platform;

    /**
     * 是否收费
     * 0-免费 1-付费 2-freemium
     * */
    private int priceType;

    /** 排序 */
    private int sort;

    /**
     * 状态
     * 1-显示 0-隐藏
     * */
    private int status;

    /**
     * 是否站长推荐
     * */
    private int isRecommend;

    /**
     * 点击次数
     * */
    private int clickCount;
}
