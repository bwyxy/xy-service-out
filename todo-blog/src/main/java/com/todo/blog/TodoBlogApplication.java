package com.todo.blog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "com.todo"
})
@EnableScheduling
@MapperScan({"com.todo.*.mapper"})
public class TodoBlogApplication {

    public static void main(String[] args) {

        SpringApplication.run(TodoBlogApplication.class, args);
        System.out.println("博客项目启动成功");
    }

}
