package com.xy.common.dto.blog;

import lombok.Data;

import java.util.List;

/**
 * 分发请求 DTO
 * 支持单篇/多篇、单平台/多平台组合分发
 * */
@Data
public class DispatchRequestDTO {

    /** 待分发文章 Id 列表（至少一个） */
    private List<Long> articleIds;

    /** 目标平台编码列表（至少一个） */
    private List<String> platformCodes;

    /** 是否忽略已成功分发过的平台（默认 true，避免重复分发） */
    private Boolean ignoreDispatched = true;
}
