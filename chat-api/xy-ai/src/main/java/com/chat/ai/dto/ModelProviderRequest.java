package com.chat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 模型提供者配置请求
 */
@Data
public class ModelProviderRequest {

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
