package com.epoint.kill.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executors;

/**
 * 定时任务多线程处理
 */
//标注其为一个配置类 其包含一个或多个bean作为配置
@Configuration
public class SchedulerConfig implements SchedulingConfigurer{


    @Override
    public void configureTasks(ScheduledTaskRegistrar scheduledTaskRegistrar) {
        //设置线程池数量为8
       scheduledTaskRegistrar.setScheduler(Executors.newScheduledThreadPool(8));
    }
}
