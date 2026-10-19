package com.chat.fg.pojo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 搜索聊天记录请求 DTO
 */
@Data
public class SearchMessageDTO {

    /**
     * 频道 ID
     */
    @NotBlank(message = "频道 Id 不能为空")
    private String channelId;

    /**
     * 频道类型 (1=个人频道, 2=群组频道)
     */
    @NotNull(message = "频道类型不能为空 1-个人频道 2-群组频道")
    private Integer channelType;

    /**
     * 搜索关键字（按类型浏览时为可选）
     */
    private String keyword;

    /**
     * 消息类型过滤，为空时默认仅文本消息
     */
    private List<Integer> types;

    /**
     * 是否仅查找链接（文本消息中包含 http/https 地址）
     */
    private Boolean linkOnly;

    /**
     * 起始时间（毫秒，含），用于按日期查找
     */
    private Long startTime;

    /**
     * 结束时间（毫秒，含），用于按日期查找
     */
    private Long endTime;

    /**
     * 返回结果条数上限
     */
    private Integer limit = 30;
}
