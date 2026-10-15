package com.xy.system.dispatch.adapter.impl;

import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchPlatformEnum;
import com.xy.system.dispatch.adapter.AbstractTemplateDispatchAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 简书平台适配器
 * 简书未提供官方开放发布接口，发布动作由平台配置中的接口地址与请求模板驱动
 * */
@Component
public class JianshuDispatchAdapter extends AbstractTemplateDispatchAdapter {

    @Override
    public String platformCode() {
        return DispatchPlatformEnum.JIANSHU.getCode();
    }

    @Override
    public String platformName() {
        return DispatchPlatformEnum.JIANSHU.getName();
    }

    @Override
    public DispatchContentFormat contentFormat() {
        return DispatchContentFormat.MARKDOWN;
    }

    @Override
    public List<String> requiredCredentialKeys() {
        return List.of("cookie");
    }

    @Override
    public void validateCredential(Map<String, String> credential) {
        requireCredentials(credential, "cookie");
    }

    @Override
    protected String defaultResultUrlTemplate() {
        return "https://www.jianshu.com/p/{id}";
    }
}
