package com.epoint.kill.server.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;


/**
 *通用rabbitmq配置
 */
@Configuration
public class RabbitmqConfig {

    private final  static Logger logger = LoggerFactory.getLogger(RabbitmqConfig.class);


    //spring 环境变量 可以获取到properties属性值
    @Autowired
    Environment env;

    //连接工厂提供连接(与rabbitmqserver相连接)
    @Autowired
    private CachingConnectionFactory connectionFactory;

    // 消费者连接工厂配置
    @Autowired
    private SimpleRabbitListenerContainerFactoryConfigurer factoryConfigurer;


    /**
     * 单一消费者实例
     */
    @Bean(name="singleListenerContainer")
    public SimpleRabbitListenerContainerFactory listenerContainerFactory(){
     SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
     factory.setConnectionFactory(connectionFactory);
     //消息转换器 使用Json转换
     factory.setMessageConverter(new Jackson2JsonMessageConverter());
     //
     factory.setConcurrentConsumers(1);
     factory.setMaxConcurrentConsumers(1);
     factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
     //factory.setPrefetchCount(1);
        /**
         * 当与acknowledgeMode AUTO 一起使用时，
         * 容器将在发送确认之前尝试处理此数量的消息(等待每个消息直到接收超时设置)。
         * 新版本中为 batchSize 容器批处理一批消息后再发送ack确认
         */
     factory.setTxSize(1);
     return factory;
    }

    /**
     *多个消费者示例
     * 使用的都是同一个消费者工厂
     * 但是bean的名称不一样
     */
   @Bean(name="multiListenerContainer")
    public SimpleRabbitListenerContainerFactory multiContainerFactory(){
    SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
     factoryConfigurer.configure(factory,connectionFactory);
     factory.setMessageConverter(new Jackson2JsonMessageConverter());

       /**
        * 消费者确认（消费者应答），当消费者收到消息处理完毕后给rabbitmq发送一个ack确认
        * NONE：不发送任何确认（ack）
        * MANUAL listener 必须通过调用Channel.basicAck()来确认所有消息
        *AUTO 容器会自动确认消息
        * 默认情况下消息消费者是自动 ack （确认）消息的
        *
        *设置为手动后需要在消费者端设置回复
        */
     factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
     //设置多少消费者用于进行消息消费
     factory.setConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.concurrency",int.class));
     factory.setMaxConcurrentConsumers(env.getProperty("spring.rabbitmq.listener.simple.max-concurrency",int.class));
       /**
        * 预拉取多少消息，即消费者端最多能保存的未确认消费数。如果超过这个数rabbitmq还未收到确认，那么mq将不会再发送消息给消费者，直到收到了确认
        *
        * 如果 AcknowledgeMode.NONE 那么忽略该属性，即使该属性设置值也无效
        */
     factory.setPrefetchCount(env.getProperty("spring.rabbitmq.listener.simple.prefetch",int.class));
     //注意以下属性与setAcknowledgeMode(AcknowledgeMode.AUTO)进行使用
    // factory.setTxSize(5);
     return  factory;
   }

   @Bean
    public RabbitTemplate rabbitTemplate(){
       connectionFactory.setPublisherConfirms(true);
       connectionFactory.setPublisherReturns(true);
       RabbitTemplate rabbitTemplate=new RabbitTemplate(connectionFactory);
       rabbitTemplate.setMandatory(true);
       rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
           @Override
           public void confirm(CorrelationData correlationData, boolean ack, String cause) {
               logger.info("消息发送成功:correlationData({}),ack({}),cause({})",correlationData,ack,cause);
           }
       });
       rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
           @Override
           public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
             logger.warn("消息发送失败:exchange({}),route({}),replyCode({}),replyText({}),message:{}",exchange,routingKey,replyCode,replyText,message);
           }
       });
       return rabbitTemplate;
   }

   //构建异步消息队列模型
    //秒杀成功异步发送邮件队列

    //@Bean注解用于告诉方法产生一个Bean对象，然后这个Bean对象交给Spring管理。产生这个Bean对象的方法Spring只会调用一次，随后这个Spring将会将这个Bean对象放在自己的IOC容器中。
    //其会调用该方法并返回一个对象，对象名称默认为方法名
    //https://www.jianshu.com/p/2f904bebb9d0
    //类似于spring的xml文件的一个<bean></bean>域
    @Bean
    public Queue successEmailQueue(){
       //相当于此处会调用该方法来创建一个对象并交给spring容器管理
        //即相当于创建队列
        return new Queue(env.getProperty("mq.kill.item.success.email.queue"),true);
    }

    //路由
    @Bean
    public TopicExchange successEmailExchange(){
       //为持久化并且不会自动删除
        //创建一个Topic类型的Exchange
       return new TopicExchange(env.getProperty("mq.kill.item.success.email.exchange"),true,false);
    }

    //绑定 将交换器（路由）绑定到该队列上并使用对应的key
    //
    @Bean
    public Binding successEmailBinding(){
       return BindingBuilder.bind(successEmailQueue()).to(successEmailExchange()).with(env.getProperty("mq.kill.item.success.email.routing.key"));
    }
    /**
     * DLX, Dead-Letter-Exchange。利用DLX, 当消息在一个队列中变成死信（dead message）之后，
     * 它能被重新publish到另一个Exchange，这个Exchange就是DLX。
     *
     * 消息变成死信有以下三种情况:
     * 1.消息被拒绝
     * 2.消息TTL过期
     * 3.队列达到最大长度
     * @return
     */
    //创建一个死信队列 其后设置死信队列
    @Bean
    public Queue successKillDeadQueue(){
        /**
         * 死信队列需要一个死信交换机和死信路由
         */
        Map<String,Object> argsmap=new HashMap<>();
        //创建一个队列 输入名称,并绑定到对应的死信交换上
        //map中的参数即为死信交换机参数

        //死信交换机名称
        argsmap.put("x-dead-letter-exchange",env.getProperty("mq.kill.item.success.kill.dead.exchange"));
        //死信队列路由键
        argsmap.put("x-dead-letter-routing-key",env.getProperty("mq.kill.item.success.kill.dead.routing.key"));
        return new Queue(env.getProperty("mq.kill.item.success.kill.dead.queue"),true,false,false,argsmap);
    }

    //创建基本交换机
    //消息首先会到基本交换机中，当超过了超时时间会放入死新交换机中
    @Bean
    public TopicExchange successKillDeadProdExchange(){
      return  new TopicExchange(env.getProperty("mq.kill.item.success.kill.dead.prod.exchange"),true,false);
    }

    //创建基本交换+基本路由-》绑定到死信对列上
    //此绑定的routing key为 Queue上的key
    @Bean
    public Binding successKillDeadProdBinding(){
       return BindingBuilder.bind(successKillDeadQueue()).to(successKillDeadProdExchange()).with(env.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
    }

    //创建真实队列
    @Bean
    public Queue successKillRealQueue(){
        //持久化队列
        return new Queue(env.getProperty("mq.kill.item.success.kill.dead.real.queue"),true);
    }

    //死信交换机
    @Bean
    public TopicExchange successKillDeadExchange(){
        return new TopicExchange(env.getProperty("mq.kill.item.success.kill.dead.exchange"),true,false);
    }

    //死信交换机+死信路由-》绑定到真正队列的绑定
    @Bean
    public Binding successKillDeadBinding(){
      return BindingBuilder.bind(successKillRealQueue()).to(successKillDeadExchange()).with(env.getProperty("mq.kill.item.success.kill.dead.routing.key"));
    }
}
