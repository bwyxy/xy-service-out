package com.todo.common.qiniu;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "qiniu")
public class QiNiuProperties {

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String domain;

    private String region;
}
