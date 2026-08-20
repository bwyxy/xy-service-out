package com.xy.common.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 在 Spring 容器初始化之前加载 .env 文件中的环境变量
 * 解决 @PostConstruct 加载时机过晚导致 ${ENV_VAR} 无法解析的问题
 */
public class DotenvApplicationContextInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment environment = context.getEnvironment();
        Map<String, Object> envMap = loadDotenv();

        if (!envMap.isEmpty()) {
            // 将 .env 变量作为最高优先级的 PropertySource 添加到 Spring Environment
            environment.getPropertySources()
                    .addFirst(new MapPropertySource("dotenvProperties", envMap));
            System.out.println("Loaded " + envMap.size() + " variables from .env file");
        }
    }

    private Map<String, Object> loadDotenv() {
        Map<String, Object> envMap = new HashMap<>();
        try {
            String projectRoot = System.getProperty("user.dir");
            Dotenv dotenv = findAndLoadDotenv(projectRoot);

            if (dotenv != null) {
                dotenv.entries().forEach(entry ->
                        envMap.put(entry.getKey(), entry.getValue())
                );
            }
        } catch (Exception e) {
            System.out.println("Warning: Failed to load .env file: " + e.getMessage());
        }
        return envMap;
    }

    private Dotenv findAndLoadDotenv(String projectRoot) {
        // 从当前工作目录向上递归查找 .env 文件（兼容多模块子项目启动）
        File currentDir = new File(projectRoot);
        int maxDepth = 10;

        for (int i = 0; i < maxDepth && currentDir != null; i++) {
            File envFile = new File(currentDir, ".env");
            if (envFile.exists()) {
                String dir = currentDir.getAbsolutePath();
                System.out.println("Loading .env from: " + envFile.getAbsolutePath());
                return Dotenv.configure().directory(dir).load();
            }
            // 到达文件系统根目录则停止
            File parent = currentDir.getParentFile();
            if (parent == null || parent.equals(currentDir)) {
                break;
            }
            currentDir = parent;
        }

        System.out.println("Warning: .env file not found in " + projectRoot + " or parent directories");
        return null;
    }
}