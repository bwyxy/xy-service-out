package com.chat.fg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 聊天模块异步线程池配置
 *
 * <p>AI 助手回复、登录日志落库等任务包含阻塞式 HTTP/DB 调用，
 * 统一交给专用线程池执行，避免占用 ForkJoinPool 公共池拖垮全局异步任务。</p>
 * */
@Configuration
public class ChatAsyncConfig {

    @Bean("chatAsyncExecutor")
    public Executor chatAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("chat-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
