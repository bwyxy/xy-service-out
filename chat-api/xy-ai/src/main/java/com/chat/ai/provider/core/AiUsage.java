package com.chat.ai.provider.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 使用统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsage {

    private Long promptTokens;

    private Long completionTokens;

    private Long totalTokens;
}
