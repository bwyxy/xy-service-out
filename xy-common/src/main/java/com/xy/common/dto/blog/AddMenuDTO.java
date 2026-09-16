package com.xy.common.dto.blog;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMenuDTO {

    @NotNull(message = "菜单名称不能为空")
    @NotEmpty(message = "菜单名称不能为空")
    private String name;

    private Long parentId = 0L;

    private Integer orderNum;

    @NotNull(message = "路由地址不能为空")
    @NotEmpty(message = "路由地址不能为空")
    private String path;

    @NotNull(message = "路由名称不能为空")
    @NotEmpty(message = "路由名称不能为空")
    private String routeName;

    private Integer isFrame = 1;

    private Integer status = 1;

    private String icon;
}
