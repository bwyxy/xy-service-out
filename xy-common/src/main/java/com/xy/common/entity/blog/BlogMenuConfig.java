package com.xy.common.entity.blog;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xy.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单信息配置表 blog_menu_config
 * */
@Data
@TableName("blog_menu_config")
@EqualsAndHashCode(callSuper = true)
public class BlogMenuConfig extends BaseEntity {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long menuId;

    private String configKey;

    private String configType;

    private String configValue;
}
