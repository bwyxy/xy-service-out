package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 友链 blog_friend_link
 * */
@Data
@TableName("blog_friend_link")
@EqualsAndHashCode(callSuper = false)
public class BlogFriendLink extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 站点名称 */
    private String name;

    /** 站点地址 */
    private String url;

    /** 站点 logo */
    private String logo;

    /** 描述 */
    private String description;

    /** 站长邮箱 */
    private String email;

    /** 排序 */
    private int sort;

    /**
     * 状态 1-显示 0-隐藏
     * */
    private int status;

    /** 备注 */
    private String remark;
}
