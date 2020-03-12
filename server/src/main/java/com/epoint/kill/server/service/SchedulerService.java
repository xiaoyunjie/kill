package com.epoint.kill.server.service;

import com.epoint.kill.model.entity.ItemKillSuccess;
import com.epoint.kill.model.mapper.ItemKillSuccessMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SchedulerService {
    private final  static Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    @Autowired
    ItemKillSuccessMapper successMapper;

    @Autowired
   Environment environment;
    //失效超时未支付订单
    //每10秒获取status为0的订单进行失效

    /**
     * 每隔10秒从数据库中去拿未支付的订单信息，并将其创建时间与当前时间进行比较，得出属性diffTime，将其与设置的超时时间相比较
     * 如果超过了设定的超时时间，即将该订单失效
     */
    //多个定时任务可能会导致多个定时任务在同一个线程中执行，需要进行配置线程池来进行多线程执行
    @Scheduled(cron = "0 0/30 * * * ?")
    public void schedulerExpireOrder(){
        try{
            List<ItemKillSuccess> orderList=successMapper.selectExpireOrders();
            if(orderList!=null&&!orderList.isEmpty()){
                for(ItemKillSuccess success:orderList){
                    //会将其进行替换
                    logger.info("获取到当前数据:{}",success);
                    //获取对应的属性并将其设置为对应的类型
                    if(success.getDiffTime()>environment.getProperty("scheduler.item.success.kill.expire",Integer.class)){
                        //处理超时的订单
                        successMapper.expireOrder(success.getCode());
                    }
                }
            }
        }catch (Exception e){
         logger.error("定时获取status为0的订单(成功未支付)，并判断其是否超过设置的TTL。，超过了即将该订单进行失效-",e.fillInStackTrace());
        }
    }



}
