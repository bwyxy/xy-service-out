package com.xy.blog.service;

import com.alibaba.fastjson2.JSON;
import com.xy.system.mapper.SysBlogConfigMapper;
import com.xy.common.entity.SysBlogConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysBlogConfigService {

    @Autowired
    private SysBlogConfigMapper sysBlogConfigMapper;

    private Map<String, Object> configMap;

    public Map<String, Object> loadAllConfigs() {
        List<SysBlogConfig> list = sysBlogConfigMapper.selectList(null);

        configMap = list.stream().collect(Collectors.toMap(
                SysBlogConfig::getConfigKey,
                item -> parseConfigValue(item.getConfigValue())
        ));
        return configMap;
    }

    private Object parseConfigValue(String value) {
        if (value == null) return null;
        // 尝试解析为 JSON 对象或数组
        try {
            return JSON.parse(value);
        } catch (Exception e) {
            // 不是合法 JSON，直接返回字符串
            return value;
        }
    }

    public Object getConfig(String key) {
        if (configMap == null) loadAllConfigs();

        return configMap.get(key);
    }

    public String getString(String key, String defaultValue) {
        Object val = getConfig(key);
        return val != null ? val.toString() : defaultValue;
    }

    public Integer getInt(String key, Integer defaultValue) {
        Object val = getConfig(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return defaultValue;
    }

    public Map<String, Object> getAllConfigs() {
        loadAllConfigs();

        return Collections.unmodifiableMap(configMap);
    }
}
