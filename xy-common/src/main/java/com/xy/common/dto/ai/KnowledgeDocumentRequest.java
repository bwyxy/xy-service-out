package com.xy.common.dto.ai;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 知识库文档新增/编辑请求
 */
@Data
public class KnowledgeDocumentRequest {

    private Long id;

    @NotNull(message = "知识库 ID 不能为空")
    private Long kbId;

    private String title;

    private String sourceType;

    private String sourceUrl;

    private String content;
}
