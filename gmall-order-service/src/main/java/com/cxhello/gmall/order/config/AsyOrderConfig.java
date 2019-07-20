package com.cxhello.gmall.order.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @author CaiXiaoHui
 * @create 2019-07-20 20:45
 */
// 异步线程池
@EnableAsync
@Configuration
public class AsyOrderConfig implements AsyncConfigurer {
    // 获取异步执行者
    @Override
    public Executor getAsyncExecutor() {
        //   配置线程池
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        // 对线程池进行设置参数
        threadPoolTaskExecutor.setCorePoolSize(10);
        // 最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(100);
        // 设置等待队列
        threadPoolTaskExecutor.setQueueCapacity(100);
        // 初始化方法
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }


    // 获取异常处理
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        // 自定义异常
        MyException myException = new MyException("呵呵有异常了！");
        myException.getMessage();
        return null;
    }

}

class MyException extends Error {
    String message;

    MyException(String msg) {
        this.message = msg;
    }

    @Override
    public String getMessage() {
        return super.getMessage();
    }
}