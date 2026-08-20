package com.chat.ai.tool.impl;

import com.chat.ai.tool.core.ToolExecutor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 当前时间工具
 */
@Slf4j
@Component("currentTimeTool")
public class CurrentTimeTool implements ToolExecutor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String execute(String argumentsJson) {
        try {
            JsonNode node = objectMapper.readTree(argumentsJson);
            String timezone = node.has("timezone") ? node.get("timezone").asText("Asia/Shanghai") : "Asia/Shanghai";
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
            return now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            log.warn("执行当前时间工具失败", e);
            return "执行失败: " + e.getMessage();
        }
    }
}
