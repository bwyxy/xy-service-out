package com.chat.fg;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.chat"
})
@EnableScheduling
public class FGApplication {

    public static void main(String[] args) {

        SpringApplication.run(FGApplication.class, args);
        System.out.println("项目启动成功");
    }
}
