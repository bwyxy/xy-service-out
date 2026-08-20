package com.chat.ai;

import com.xy.system.service.impl.SysAiApiKeyServiceImpl;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@ComponentScan(basePackages = {"com.chat.ai", "com.xy.common"})
@MapperScan({"com.chat.ai.mapper", "com.xy.system.mapper"})
@Import(SysAiApiKeyServiceImpl.class)
public class XyAiApplication {

    public static void main(String[] args) {

        SpringApplication.run(XyAiApplication.class, args);
        System.out.println("项目启动成功, 喝杯咖啡～～～");
    }

}
