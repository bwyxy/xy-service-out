package com.xy.common.vo.ai;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库权限视图对象
 */
@Data
public class KnowledgeBasePermissionVo {

    private Long id;

    private Long kbId;

    private String targetType;

    private Long targetId;

    private String targetName;

    private String permission;

    private LocalDateTime createTime;
}
