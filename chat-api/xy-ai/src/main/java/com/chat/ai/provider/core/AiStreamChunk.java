package com.chat.ai.provider.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 流式输出块
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiStreamChunk {

    private String content;

    private Boolean done;

    private AiUsage usage;

    private String errorMsg;
}
