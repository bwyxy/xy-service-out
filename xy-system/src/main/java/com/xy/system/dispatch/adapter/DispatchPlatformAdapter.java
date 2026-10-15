package com.xy.system.dispatch.adapter;

import com.xy.common.entity.blog.BlogDispatchPlatform;
import com.xy.common.enums.DispatchContentFormat;
import com.xy.system.dispatch.adapter.model.ImageUploadRequest;
import com.xy.system.dispatch.adapter.model.PublishContext;
import com.xy.system.dispatch.adapter.model.PublishResult;
import com.xy.system.dispatch.adapter.model.StatFetchRequest;
import com.xy.system.dispatch.adapter.model.StatResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 分发平台适配器
 * 新增平台只需实现本接口并声明为 Spring Bean，即可被适配器注册中心自动纳管
 * */
public interface DispatchPlatformAdapter {

    /**
     * 平台编码，需与 blog_dispatch_platform.platform_code 一致
     * */
    String platformCode();

    /**
     * 平台名称
     * */
    String platformName();

    /**
     * 平台正文格式要求，内容适配层据此产出正文
     * */
    DispatchContentFormat contentFormat();

    /**
     * 该平台必须配置的凭证字段名，用于前端渲染配置表单
     * */
    List<String> requiredCredentialKeys();

    /**
     * 校验凭证完整性，缺失或格式非法时抛出 DispatchException
     * */
    void validateCredential(Map<String, String> credential);

    /**
     * 平台是否提供正文图片上传接口
     * */
    default boolean supportsImageUpload() {
        return false;
    }

    /**
     * 上传正文图片到平台图床，返回可在正文中直接使用的图片地址
     * */
    default String uploadContentImage(ImageUploadRequest request) {
        return null;
    }

    /**
     * 发布文章
     * */
    PublishResult publish(PublishContext context);

    /**
     * 是否支持运营数据采集
     * */
    default boolean supportsStatFetch() {
        return false;
    }

    /**
     * 采集已发布文章的运营数据
     * */
    default List<StatResult> fetchStats(StatFetchRequest request) {
        return Collections.emptyList();
    }

    /**
     * 连通性测试，默认只做凭证完整性校验
     * */
    default void testConnection(Map<String, String> credential, Map<String, Object> config,
                                BlogDispatchPlatform platform, int timeoutSeconds) {
        validateCredential(credential);
    }
}
