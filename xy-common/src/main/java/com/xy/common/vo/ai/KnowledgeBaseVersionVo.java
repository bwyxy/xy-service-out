package com.xy.common.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库版本视图对象
 */
@Data
public class KnowledgeBaseVersionVo {

    private Long id;

    private Long kbId;

    private String version;

    private String description;

    private Integer docCount;

    private Integer status;

    private LocalDateTime publishedTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
