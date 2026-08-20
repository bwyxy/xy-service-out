package com.chat.ai.provider.core;

import com.chat.ai.exception.AiException;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型提供者工厂
 */
@Component
public class ModelProviderFactory {

    private final List<ModelProvider> providers;

    private final Map<String, ModelProvider> providerMap = new ConcurrentHashMap<>();

    public ModelProviderFactory(List<ModelProvider> providers) {
        this.providers = providers;
    }

    @PostConstruct
    public void init() {
        for (ModelProvider provider : providers) {
            providerMap.put(provider.getCode(), provider);
        }
    }

    public ModelProvider getProvider(String code) {
        ModelProvider provider = providerMap.get(code);
        if (provider == null) {
            throw new AiException("不支持的模型提供者: " + code);
        }
        return provider;
    }

    public boolean hasProvider(String code) {
        return providerMap.containsKey(code);
    }
}
