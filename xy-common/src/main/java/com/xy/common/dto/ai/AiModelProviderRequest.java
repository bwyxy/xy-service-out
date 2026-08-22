package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 模型提供者新增/编辑请求
 */
@Data
public class AiModelProviderRequest {

    private Long id;

    @NotBlank(message = "提供者名称不能为空")
    private String name;

    @NotBlank(message = "提供者编码不能为空")
    private String code;

    @NotBlank(message = "提供者类型不能为空")
    private String type;

    private String endpoint;

    private String apiKey;

    private String model;

    private Integer priority;

    private Integer enabled;

    private String configJson;
}
