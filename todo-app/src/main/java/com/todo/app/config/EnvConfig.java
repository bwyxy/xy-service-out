package com.todo.app.config;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void loadEnv() {
        try {
            // 尝试从项目根目录加载.env文件
            String projectRoot = System.getProperty("user.dir");
            File envFile = new File(projectRoot, ".env");

            if (envFile.exists()) {
                Dotenv dotenv = Dotenv.configure()
                        .directory(projectRoot)
                        .load();

                // 将.env文件中的环境变量设置到系统环境变量中
                dotenv.entries().forEach(entry -> {
                    if (System.getenv(entry.getKey()) == null) {
                        System.setProperty(entry.getKey(), entry.getValue());
                    }
                });
                System.out.println("Successfully loaded .env file from: " + envFile.getAbsolutePath());
            } else {
                // 尝试从上级目录加载
                File parentEnvFile = new File(projectRoot + File.separator + "..", ".env");
                if (parentEnvFile.exists()) {
                    Dotenv dotenv = Dotenv.configure()
                            .directory(parentEnvFile.getParent())
                            .load();

                    // 将.env文件中的环境变量设置到系统环境变量中
                    dotenv.entries().forEach(entry -> {
                        if (System.getenv(entry.getKey()) == null) {
                            System.setProperty(entry.getKey(), entry.getValue());
                        }
                    });
                    System.out.println("Successfully loaded .env file from: " + parentEnvFile.getAbsolutePath());
                } else {
                    System.out.println("Warning: .env file not found in project root or parent directory");
                }
            }
        } catch (Exception e) {
            // 如果加载失败，打印日志但不影响应用启动
            System.out.println("Warning: .env file not found or error loading: " + e.getMessage());
        }
    }

}
