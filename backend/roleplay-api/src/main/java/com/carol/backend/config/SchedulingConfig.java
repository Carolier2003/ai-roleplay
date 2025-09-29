package com.carol.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * 定时任务配置类
 * 启用Spring定时任务功能并配置线程池
 */
@Slf4j
@Configuration
@EnableScheduling
public class SchedulingConfig implements SchedulingConfigurer {
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 设置定时任务使用的线程池
        // 避免定时任务阻塞主线程
        taskRegistrar.setScheduler(Executors.newScheduledThreadPool(5));
        
        log.info("[SchedulingConfig] 定时任务配置完成，使用线程池大小: 5");
    }
}
