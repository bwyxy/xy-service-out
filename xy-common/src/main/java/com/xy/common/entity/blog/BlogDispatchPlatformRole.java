package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分发平台角色权限表 blog_dispatch_platform_role
 * 用于按角色控制可使用哪些分发平台，超级管理员不受限制
 * */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("blog_dispatch_platform_role")
public class BlogDispatchPlatformRole extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 平台编码 */
    private String platformCode;

    /** 角色 Id */
    private Long roleId;
}
