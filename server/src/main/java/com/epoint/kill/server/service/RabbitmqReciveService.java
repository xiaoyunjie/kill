package com.epoint.kill.server.service;

import com.epoint.kill.model.dto.KillSuccessUserInfo;
import com.epoint.kill.model.dto.MailDto;
import com.epoint.kill.model.entity.ItemKillSuccess;
import com.epoint.kill.model.mapper.ItemKillSuccessMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

//消费者端
@Service
public class RabbitmqReciveService {
    private final  static Logger logger = LoggerFactory.getLogger(RabbitmqReciveService.class);

    @Autowired
    MailService mailService;

    @Autowired
    Environment environment;

    @Autowired
    ItemKillSuccessMapper successMapper;

    // 使用该注解监听哪些队列
    //监听配置文件中创建的队列 使用哪一个消费者实例
    //秒杀成功接受消息，异步邮件通知
    //queues 监听哪些队列 containerFactory 使用哪个消费者实例

    /**
     * 使用RabbitListenerContainerFactory为每个带注释的方法在后台创建消息 listener 容器。
     * 此处SimpleRabbitListenerContainerFactory 为其实现类
     * queues 为要监听的队列名称
     * containerFactory 为要创建listener容器的工厂 此处直接写bean的名称即可
     * 注意在此种情况 该队列必须已经绑定到了某个交换器上
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.email.queue}"},containerFactory = "singleListenerContainer")
    public void consumeEmailMsg(KillSuccessUserInfo info){
     try{
         //其会将info的内容放入前面的{}中
       logger.info("秒杀异步通知-接收到的消息:{}",info);
       //发送邮件逻辑
         MailDto dto=new MailDto();
          dto.setSubject(environment.getProperty("mail.kill.success.subject"));
          dto.setContent(environment.getProperty("mail.kill.success.content"));
          dto.setTos(new String[]{info.getEmail()});
          mailService.sendHTMLmail(dto);
     }catch (Exception e){
         logger.error("接收消息出错",e.fillInStackTrace());
     }
    }

    //监听用户秒杀成功之后超时未支付

    /**
     * 失效超时未支付的订单
     * @param info
     */
    @RabbitListener(queues = {"${mq.kill.item.success.kill.dead.real.queue}"},containerFactory = "singleListenerContainer")
    public void consumeExpireOrder(KillSuccessUserInfo info){
        try{
            //其会将info的内容放入前面的{}中
            logger.info("秒杀成功后超时未支付--监听者-接收消息:{}",info);
            /**
             * 查询该订单号详细信息
             * 用于失效该订单
             */
            if(info!=null){
                // 0为未支付 1为已付款 2为已取消 -1 为失效
               if(info.getStatus().intValue()==0){
                 //失效订单记录
                    successMapper.expireOrder(info.getCode());
               }
            }
        }catch (Exception e){
            logger.error("接收消息出错",e.fillInStackTrace());
        }
    }

}
