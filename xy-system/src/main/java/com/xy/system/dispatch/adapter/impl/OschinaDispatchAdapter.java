package com.xy.system.dispatch.adapter.impl;

import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchPlatformEnum;
import com.xy.system.dispatch.adapter.AbstractTemplateDispatchAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 开源中国(OSChina)平台适配器
 * 开源中国未提供官方开放发布接口，发布动作由平台配置中的接口地址与请求模板驱动
 * */
@Component
public class OschinaDispatchAdapter extends AbstractTemplateDispatchAdapter {

    @Override
    public String platformCode() {
        return DispatchPlatformEnum.OSCHINA.getCode();
    }

    @Override
    public String platformName() {
        return DispatchPlatformEnum.OSCHINA.getName();
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
        return "https://my.oschina.net/u/{account}/blog/{id}";
    }
}
