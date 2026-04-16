package com.todo.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.ManagedBean;

@SpringBootApplication(scanBasePackages = {
        "com.todo"
})
@EnableScheduling
@MapperScan({"com.todo.system.mapper", "com.todo.*.mapper"})
public class TodoAdminApplication {

    public static void main(String[] args) {

        SpringApplication.run(TodoAdminApplication.class, args);
        System.out.println("项目启动成功");
    }

}
