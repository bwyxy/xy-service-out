package com.xy.common.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库文档视图对象
 */
@Data
public class KnowledgeDocumentVo {

    private Long id;

    private Long kbId;

    private String title;

    private String sourceType;

    private String sourceUrl;

    private String content;

    private Integer chunkCount;

    private Integer status;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
