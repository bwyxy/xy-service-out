package com.chat.ai.tool.core;

import com.chat.ai.entity.AiTool;
import com.chat.ai.mapper.AiToolMapper;
import com.chat.ai.provider.core.ModelProvider;
import com.chat.ai.provider.core.ModelProviderFactory;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工具注册中心
 */
@Slf4j
@Component
public class ToolRegistry {

    private final AiToolMapper toolMapper;

    private final ApplicationContext applicationContext;

    private final ModelProviderFactory providerFactory;

    private final Map<String, ToolExecutor> executors = new ConcurrentHashMap<>();

    public ToolRegistry(AiToolMapper toolMapper, ApplicationContext applicationContext,
                        ModelProviderFactory providerFactory) {
        this.toolMapper = toolMapper;
        this.applicationContext = applicationContext;
        this.providerFactory = providerFactory;
    }

    @PostConstruct
    public void init() {
        refreshTools();
    }

    /**
     * 注册工具执行器
     */
    public void registerExecutor(String code, ToolExecutor executor) {
        executors.put(code, executor);
    }

    /**
     * 从数据库刷新工具到所有模型提供者
     */
    public void refreshTools() {
        List<AiTool> tools = toolMapper.selectList(null);
        for (ModelProvider provider : getProviders()) {
            for (AiTool tool : tools) {
                if (tool.getEnabled() == null || tool.getEnabled() != 1) {
                    continue;
                }
                ToolExecutor executor = resolveExecutor(tool);
                if (executor == null) {
                    log.warn("工具 {} 未找到执行器", tool.getCode());
                    continue;
                }
                try {
                    provider.registerTool(tool.getCode(), tool.getDescription(),
                            tool.getParametersSchema(), executor::execute);
                    log.debug("已注册工具 {} 到提供者 {}", tool.getCode(), provider.getCode());
                } catch (Exception e) {
                    log.warn("注册工具 {} 到提供者 {} 失败", tool.getCode(), provider.getCode(), e);
                }
            }
        }
    }

    /**
     * 执行工具调用
     */
    public String execute(String code, String argumentsJson) {
        ToolExecutor executor = executors.get(code);
        if (executor == null) {
            return "错误：未找到工具 " + code;
        }
        return executor.execute(argumentsJson);
    }

    private ToolExecutor resolveExecutor(AiTool tool) {
        // 优先从 Spring 容器中获取
        if (tool.getImplementationBean() != null && !tool.getImplementationBean().isEmpty()) {
            Object bean = null;
            try {
                bean = applicationContext.getBean(tool.getImplementationBean());
            } catch (Exception e) {
                log.warn("未找到工具 Bean: {}", tool.getImplementationBean());
            }
            if (bean instanceof ToolExecutor executor) {
                return executor;
            }
        }
        return executors.get(tool.getCode());
    }

    private List<ModelProvider> getProviders() {
        return providerFactory.hasProvider("openai")
                ? List.of(providerFactory.getProvider("openai"))
                : List.of();
    }
}
