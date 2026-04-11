package com.todo.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.ManagedBean;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class },
scanBasePackages = {
        "com.todo.admin.*"
})
@EnableScheduling
@ManagedBean("com.todo.admin.mapper")
public class TodoAdminApplication {

    public static void main(String[] args) {

        SpringApplication.run(TodoAdminApplication.class, args);
        System.out.println("项目启动成功");
    }

}
