package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库权限授权请求
 */
@Data
public class KnowledgeBasePermissionRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    @NotBlank(message = "授权对象类型不能为空")
    private String targetType;

    @NotNull(message = "授权对象 ID 不能为空")
    private Long targetId;

    @NotBlank(message = "权限类型不能为空")
    private String permission;
}
