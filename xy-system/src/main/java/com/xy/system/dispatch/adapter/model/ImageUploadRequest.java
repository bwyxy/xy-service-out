package com.xy.system.dispatch.adapter.model;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 正文图片上传到平台图床的请求
 * */
@Data
public class ImageUploadRequest {

    /** 图片字节内容 */
    private byte[] bytes;

    /** 文件名 */
    private String fileName;

    /** 平台账号凭证（已解密） */
    private Map<String, String> credential = new HashMap<>();

    /** 平台发布默认规则 */
    private Map<String, Object> config = new HashMap<>();

    /** 平台配置 */
    private BlogDispatchPlatform platform;

    /** 请求超时（秒） */
    private int timeoutSeconds = 15;
}
