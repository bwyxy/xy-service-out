package com.xy.system.dispatch.adapter.impl;

import com.xy.common.enums.DispatchContentFormat;
import com.xy.common.enums.DispatchPlatformEnum;
import com.xy.system.dispatch.adapter.AbstractTemplateDispatchAdapter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 知乎平台适配器
 * 知乎未提供官方开放写入接口，正文以 HTML 富文本提交
 * */
@Component
public class ZhihuDispatchAdapter extends AbstractTemplateDispatchAdapter {

    @Override
    public String platformCode() {
        return DispatchPlatformEnum.ZHIHU.getCode();
    }

    @Override
    public String platformName() {
        return DispatchPlatformEnum.ZHIHU.getName();
    }

    @Override
    public DispatchContentFormat contentFormat() {
        return DispatchContentFormat.HTML;
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
        return "https://zhuanlan.zhihu.com/p/{id}";
    }
}
