package com.epoint.kill.server.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
//redis 通用配置
public class RedisConfig {

    @Autowired
   private RedisConnectionFactory connectionFactory;

    //配置RedisTemplete
    @Bean
    public RedisTemplate<String,Object> redisTemplate(){
         RedisTemplate<String,Object> redisTemplate=new RedisTemplate<>();
         redisTemplate.setConnectionFactory(connectionFactory);
         //TODO:指定key  vaule 序列化规则
           redisTemplate.setKeySerializer(new StringRedisSerializer());
           redisTemplate.setValueSerializer(new JdkSerializationRedisSerializer());

           redisTemplate.setHashKeySerializer(new StringRedisSerializer());
           return redisTemplate;

    }

   @Bean
    public StringRedisTemplate stringRedisTemplate(){
        StringRedisTemplate stringRedisTemplate=new StringRedisTemplate();
       stringRedisTemplate.setConnectionFactory(connectionFactory);
        return  stringRedisTemplate;
  }


}
