package com.chat.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.chat.ai", "com.xy.common", "com.xy.framework"})
@MapperScan("com.chat.ai.mapper")
public class XyAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(XyAiApplication.class, args);
    }

}
