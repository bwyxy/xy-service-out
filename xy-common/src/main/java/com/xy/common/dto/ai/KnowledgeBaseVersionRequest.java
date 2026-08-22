package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库版本新增/编辑请求
 */
@Data
public class KnowledgeBaseVersionRequest {

    private Long id;

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    @NotBlank(message = "版本号不能为空")
    private String version;

    private String description;
}
