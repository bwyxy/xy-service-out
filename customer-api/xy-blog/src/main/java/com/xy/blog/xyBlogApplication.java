package com.xy.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.xy"
})
@EnableScheduling
@MapperScan({"com.xy.*.mapper"})
public class xyBlogApplication {

    public static void main(String[] args) {

        SpringApplication.run(xyBlogApplication.class, args);
        System.out.println("博客项目启动成功");
    }

}
