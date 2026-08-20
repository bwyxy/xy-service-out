package com.chat.fg;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.chat",
        "com.xy"
})
@MapperScan({"com.xy.*.mapper", "com.chat.*.mapper"})
@EnableScheduling
@EnableFeignClients(basePackages = {"com.xy.common.client", "com.chat.fg.client"})
public class FGApplication {

    public static void main(String[] args) {

        SpringApplication.run(FGApplication.class, args);
        System.out.println("项目启动成功");
    }
}
