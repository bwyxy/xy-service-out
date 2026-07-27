package com.xy.common.client.dto.response;

import lombok.Data;

@Data
public class ApiResult {

    /**
     * 请求状态
     * 200-成功 400-请求参数错误 500-服务器内部错误
     * */
    private Integer status;

    private String msg;
}
