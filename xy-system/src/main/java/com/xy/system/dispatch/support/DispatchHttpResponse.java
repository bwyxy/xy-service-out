package com.xy.system.dispatch.support;

import lombok.Data;

/**
 * 分发 HTTP 响应
 * */
@Data
public class DispatchHttpResponse {

    /** HTTP 状态码 */
    private int status;

    /** 响应体 */
    private String body;

    /** 响应耗时（毫秒） */
    private long costMs;

    public DispatchHttpResponse(int status, String body, long costMs) {
        this.status = status;
        this.body = body;
        this.costMs = costMs;
    }

    /** 是否 HTTP 层成功 */
    public boolean isSuccess() {
        return status >= 200 && status < 300;
    }
}
