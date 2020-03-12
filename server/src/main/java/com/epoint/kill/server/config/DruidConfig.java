package com.epoint.kill.server.config;

//数据源配置文件 让其配置生效

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.support.http.StatViewServlet;
import com.alibaba.druid.support.http.WebStatFilter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class DruidConfig {
    /**
     * 创建数据源
     * @return
     * 将该连接池对象放入容器中
     * ConfigurationProperties
     * 告诉SpringBoot将本类中的所有属性和配置文件中相关的配置进行绑定；
    　prefix = "xxx"：配置文件中哪个下面的所有属性进行一一映射
    只有这个组件是容器中的组件，才能容器提供的@ConfigurationProperties功能；
     @ConfigurationProperties(prefix = "xxx")默认从全局配置文件中获取值
     */
    @ConfigurationProperties(prefix = "spring.datasource")
    @Bean //将该对象添加到容器中
    public DataSource druid(){
        //因为此类中所有属性和配置文件一一对应 所以可以直接绑定
        return  new DruidDataSource();
    }
    /**
     * 配置druid监控
     * 1 配置管理后台的servlet
     * 2 配置一个监控的filter
     */
    //注册一个servlet
    @Bean
    public ServletRegistrationBean statViewServlet(){
        ServletRegistrationBean bean= new ServletRegistrationBean(new StatViewServlet(),"/druid/*");
        /**
         * 配置初始化参数 具体的初始化参数进入StatViewServlet中的父类ResourceServlet查看
         */
        // 初始化参数map
        Map<String,String> initParams=new HashMap<>();
        /**
         * 配置登录后台的用户名和密码
         */
        initParams.put("loginUsername","admin");
        initParams.put("loginPassword","123456");
        /**
         * 允许哪些用户登录
         * deny拒绝哪些用户不允许访问 可以配置ip地址
         *
         */
        //允许所有用户登录
        initParams.put("allow","");
        bean.setInitParameters(initParams);
        return  bean;
    }
    /**
     * 配置一个web监控的filter
     */
     @Bean
      public FilterRegistrationBean webStatFilter(){
          //生成FilterRegistrationBean对象 要注册哪个filter就注册哪个
          FilterRegistrationBean bean = new FilterRegistrationBean();
          //配置WebStatFilter过滤器
          bean.setFilter(new WebStatFilter());
          Map<String,String> initParams =new HashMap<>();
          //此处属性配置在WebStatFilter的属性中 不拦截哪些请求
          initParams.put("exclusions","*.js,*.css,/druid/*");
          //设置初始化参数
          bean.setInitParameters(initParams);
          //设置拦截哪些请求 放入参数为collection 过滤所有请求
          //aslist 方法将存入的参数返会为对应类型的列表（list）
          bean.setUrlPatterns(Arrays.asList("/*"));
          return  bean;
      }
}
