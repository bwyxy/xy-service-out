package com.xy.common.client.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResult<T> {

    /**
     * 请求状态
     * 200-成功 400-请求参数错误 500-服务器内部错误
     * */
    private Integer status;

    private T data;

    private String msg;
}
