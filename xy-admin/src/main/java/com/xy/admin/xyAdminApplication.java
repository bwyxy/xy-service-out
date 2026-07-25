package com.xy.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.xy"
})
@EnableScheduling
@MapperScan({"com.xy.system.mapper", "com.xy.*.mapper"})
public class xyAdminApplication {

    public static void main(String[] args) {

        SpringApplication.run(xyAdminApplication.class, args);
        System.out.println("项目启动成功");
    }

}
