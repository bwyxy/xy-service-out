package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库新增/编辑请求
 */
@Data
public class KnowledgeBaseRequest {

    private Long id;

    @NotBlank(message = "知识库名称不能为空")
    private String name;

    private String description;

    private String embeddingModel;

    private String providerCode;
}
