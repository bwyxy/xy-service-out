package com.xy.common.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IMAddrResponse {

    /**
     * tcp 连接地址
     * */
    @JsonProperty("tcp_addr")
    private String tcpAddr;

    /**
     * ws 连接地址
     * */
    @JsonProperty("ws_addr")
    private String wsAddr;

    /**
     * wss 连接地址
     * */
    @JsonProperty("wss_addr")
    private String wssAddr;
}
