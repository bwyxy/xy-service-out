package com.chat.ai.provider.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 模型同步响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiModelResponse {

    private String content;

    private String model;

    private AiUsage usage;

    private String finishReason;
}
