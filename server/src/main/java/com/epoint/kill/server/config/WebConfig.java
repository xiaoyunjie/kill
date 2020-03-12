package com.epoint.kill.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {
    /**
     * ctrl o 重写
     * @param registry
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //视图解析器 注意如果没有导入thyemlaf springboot不会自动渲染（未配置视图解析器）
        registry.addViewController("/login").setViewName("login");
        registry.addViewController("/index").setViewName("index");
    }

}
