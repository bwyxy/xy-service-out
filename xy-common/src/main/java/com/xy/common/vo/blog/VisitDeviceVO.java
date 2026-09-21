package com.xy.common.vo.blog;

import lombok.Data;

/**
 * 访问统计-设备分布
 */
@Data
public class VisitDeviceVO {

    /**
     * 设备类型
     */
    private String device;

    /**
     * 数量
     */
    private Long count;
}
