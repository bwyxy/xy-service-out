package com.xy.system.dispatch.adapter.model;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运营数据采集请求
 * */
@Data
public class StatFetchRequest {

    /** 待采集的文章引用 */
    private List<PublishedRef> refs = new ArrayList<>();

    /** 平台账号凭证（已解密） */
    private Map<String, String> credential = new HashMap<>();

    /** 平台发布默认规则 */
    private Map<String, Object> config = new HashMap<>();

    /** 平台配置 */
    private BlogDispatchPlatform platform;

    /** 请求超时（秒） */
    private int timeoutSeconds = 15;
}
