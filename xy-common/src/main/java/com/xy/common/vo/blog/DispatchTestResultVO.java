package com.xy.common.vo.blog;

import lombok.Data;

/**
 * 分发平台连通性测试结果 VO
 * */
@Data
public class DispatchTestResultVO {

    /** 是否通过 */
    private Boolean success;

    /** 结果说明 */
    private String message;

    /** 耗时（毫秒） */
    private Long costMs;
}
