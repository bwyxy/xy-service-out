package com.xy.common.dto.forum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 处理举报
 * */
@Data
public class ClearReportDTO {

    /**
     * 举报 ID
     * */
    @NotNull(message = "举报 ID不能为空")
    private Long reportId;

    /**
     * 处理结果
     * */
    @NotNull(message = "处理结果不能为空")
    @NotBlank(message = "处理结果不能为空")
    private String result;
}
