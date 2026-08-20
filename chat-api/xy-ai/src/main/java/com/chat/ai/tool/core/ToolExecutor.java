package com.chat.ai.tool.core;

/**
 * 工具执行器接口
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * 执行工具调用
     *
     * @param argumentsJson 工具参数 JSON
     * @return 工具执行结果 JSON 或文本
     */
    String execute(String argumentsJson);
}
