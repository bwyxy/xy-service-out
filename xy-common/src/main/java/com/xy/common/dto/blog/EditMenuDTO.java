package com.xy.common.dto.blog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EditMenuDTO extends AddMenuDTO {

    @NotNull(message = "菜单Id不能为空")
    @NotBlank(message = "菜单Id不能为空")
    private Long id;
}
