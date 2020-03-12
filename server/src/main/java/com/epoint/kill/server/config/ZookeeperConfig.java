package com.epoint.kill.server.config;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.Environment;
import org.apache.zookeeper.ZooKeeper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZookeeperConfig {

    @Autowired
    org.springframework.core.env.Environment env;

    @Bean
    //自定义注入Zookeeper客户端操作实例
    public CuratorFramework curatorFramework(){
       CuratorFramework curatorFramework=CuratorFrameworkFactory.builder()
                .connectString(env.getProperty("zookeeper.host"))
                .namespace(env.getProperty("zookeeper.namespace"))
                .retryPolicy(new RetryNTimes(5,1000))
                .build();
       curatorFramework.start();
      return curatorFramework;
    }


}
