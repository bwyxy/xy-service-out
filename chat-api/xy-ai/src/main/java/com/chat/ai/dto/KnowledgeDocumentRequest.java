package com.chat.ai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库文档添加请求
 */
@Data
public class KnowledgeDocumentRequest {

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    private String title;

    private String sourceType;

    private String sourceUrl;

    private String content;
}
