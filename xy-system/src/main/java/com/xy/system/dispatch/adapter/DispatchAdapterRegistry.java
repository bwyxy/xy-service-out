package com.xy.system.dispatch.adapter;

import com.xy.system.dispatch.DispatchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 分发平台适配器注册中心
 * 自动收集容器中所有 DispatchPlatformAdapter 实现，按平台编码索引
 * */
@Slf4j
@Component
public class DispatchAdapterRegistry {

    private final Map<String, DispatchPlatformAdapter> adapterMap = new LinkedHashMap<>();

    public DispatchAdapterRegistry(List<DispatchPlatformAdapter> adapters) {
        if (adapters != null) {
            for (DispatchPlatformAdapter adapter : adapters) {
                if (adapter.platformCode() == null) {
                    continue;
                }
                adapterMap.put(adapter.platformCode().toLowerCase(Locale.ROOT), adapter);
            }
        }
        log.info("分发平台适配器注册完成，共 {} 个：{}", adapterMap.size(), adapterMap.keySet());
    }

    /**
     * 获取适配器，不存在时抛出 ADAPTER_NOT_FOUND
     * */
    public DispatchPlatformAdapter get(String platformCode) {
        DispatchPlatformAdapter adapter = find(platformCode);
        if (adapter == null) {
            throw DispatchException.notFound("未找到平台【" + platformCode + "】的适配器实现");
        }
        return adapter;
    }

    /**
     * 获取适配器，不存在返回 null
     * */
    public DispatchPlatformAdapter find(String platformCode) {
        if (platformCode == null) {
            return null;
        }
        return adapterMap.get(platformCode.toLowerCase(Locale.ROOT));
    }

    /**
     * 是否存在对应平台的适配器
     * */
    public boolean supports(String platformCode) {
        return find(platformCode) != null;
    }

    /**
     * 已注册的平台编码
     * */
    public List<String> supportedCodes() {
        return new ArrayList<>(adapterMap.keySet());
    }

    /**
     * 全部适配器
     * */
    public List<DispatchPlatformAdapter> all() {
        return Collections.unmodifiableList(new ArrayList<>(adapterMap.values()));
    }
}
