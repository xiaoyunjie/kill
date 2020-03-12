package com.epoint.kill.server.service;

import com.epoint.kill.model.dto.KillSuccessUserInfo;
import com.epoint.kill.model.mapper.ItemKillSuccessMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.AbstractJavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
//发送消息服务
public class RabbitmqSenderService {
    private final static Logger logger = LoggerFactory.getLogger(RabbitmqSenderService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    Environment environment;

    @Autowired
    private ItemKillSuccessMapper successMapper;


    //秒杀成功异步发送邮件通知消息
    //传入订单编号
    public void sendKillSuccessEmailMsg(String orderNo) {
        logger.info("准备发送消息:{}", orderNo);

        try {
            if (StringUtils.isNoneBlank(orderNo)) {
                //获取当前商品的信息
                KillSuccessUserInfo userInfo = successMapper.selectByCode(orderNo);
                if (userInfo != null) {
                    //rabbitMq发送消息逻辑
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
                    rabbitTemplate.setExchange(environment.getProperty("mq.kill.item.success.email.exchange"));
                    //设置消息的路由键   如果消息的路由键和Exchange中的某个路由键想匹配，则交换器会将其发送到对应的队列中
                    // 即可以理解为消息的路由键和队列的路由键相匹配即可
                    rabbitTemplate.setRoutingKey(environment.getProperty("mq.kill.item.success.email.routing.key"));
                    //将info充当消息进行发送
                    rabbitTemplate.convertAndSend(userInfo, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            //获取消息的属性
                            MessageProperties messageProperties = message.getMessageProperties();
                            //设置消息持久化
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            //设置消息头，便于消费者在接收时可以直接用该类进行接收
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);
                            return message;
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("秒杀成功发送消息异常. 消息为：{}", orderNo, e.fillInStackTrace());
        }
    }

    //发送抢购成功之后超时订单消息
    //抢购成功后，将
    public void sendKillSuccessOrderExpirMsg(final String orderNo){
        logger.info("将订单信息发送到死信队列中");
        try{
            if(StringUtils.isNoneBlank()){
            KillSuccessUserInfo info=successMapper.selectByCode(orderNo);
                if (info != null) {
                    //rabbitMq发送消息逻辑
                    rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

                    //先将其发送到基本交换机中
                    rabbitTemplate.setExchange(environment.getProperty("mq.kill.item.success.kill.dead.prod.exchange"));
                    //设置消息的路由
                    rabbitTemplate.setRoutingKey(environment.getProperty("mq.kill.item.success.kill.dead.prod.routing.key"));
                    //将info充当消息进行发送
                    rabbitTemplate.convertAndSend(info, new MessagePostProcessor() {
                        @Override
                        public Message postProcessMessage(Message message) throws AmqpException {
                            //获取消息的属性
                            MessageProperties messageProperties = message.getMessageProperties();
                            messageProperties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);

                            //设置TTL时间
                             messageProperties.setExpiration(environment.getProperty("mq.kill.item.success.kill.expire"));
                            //设置消息头，便于消费者在接收时可以直接用该类进行接收
                            messageProperties.setHeader(AbstractJavaTypeMapper.DEFAULT_CONTENT_CLASSID_FIELD_NAME, KillSuccessUserInfo.class);

                            return message;
                        }
                    });
                }


            }
        }catch (Exception e){
           logger.info("发送失败，消息未发送成功");
        }


    }


}



