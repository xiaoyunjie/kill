package com.epoint.kill.server.service.impl;

import com.epoint.kill.model.dto.KillSuccessUserInfo;
import com.epoint.kill.model.entity.Item;
import com.epoint.kill.model.entity.ItemKill;
import com.epoint.kill.model.entity.ItemKillSuccess;
import com.epoint.kill.model.mapper.ItemKillMapper;
import com.epoint.kill.model.mapper.ItemKillSuccessMapper;
import com.epoint.kill.model.mapper.ItemMapper;
import com.epoint.kill.server.service.ItemService;
import com.epoint.kill.server.service.RabbitmqReciveService;
import com.epoint.kill.server.service.RabbitmqSenderService;
import com.epoint.kill.server.utils.RandomUtil;
import com.epoint.kill.server.utils.SnowFlake;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMultiLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ItemServiceImpl implements ItemService {

    private final static Logger logger = LoggerFactory.getLogger(ItemServiceImpl.class);

    @Autowired
    //获取秒杀商品列表
    ItemKillMapper killMapper;

    //商品详情对象
    @Autowired
    ItemMapper itemMapper;

    @Autowired
    ItemKillSuccessMapper successMapper;

    @Autowired
    SnowFlake flake;

    @Autowired
    RabbitmqSenderService rabbitmqSenderService;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    CuratorFramework curatorFramework;

    private static final String pathPrefix="/kill/zkLock";
    @Override
    //获取所有秒杀商品
    public List<ItemKill> getItemKills() {

        return killMapper.selectAll();
    }

    @Override
    /**
     * 秒杀成功后更新秒杀商品数量
     * 并生成秒杀订单通知用户
     */
    public boolean execKill(String id, Integer userId) {
        Integer killId = Integer.parseInt(id);
        //查看当前用户是否已获得秒杀商品，获得后不可秒杀
        /**
         *即生成订单数据的逻辑没有判断的逻辑快
         */
        if (successMapper.countByKillUserId(killId, userId) <= 0) {
            ItemKill itemKill = killMapper.selectById(killId);
            //判断当前商品是否能被秒杀
            if (itemKill != null && itemKill.getCanKill() == 1) {
                //更新秒杀商品数返回true
                //扣减库存 保证商品总数不小于0
                int res = killMapper.updateKillItem(killId);

                if (res > 0) {
                    //扣减成功,生成订单
                    commonRecordKillSuccess(itemKill, userId);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Item getItemById(String id) {
        return itemMapper.selectByPrimaryKey(Integer.parseInt(id));
    }

    @Override
    public KillSuccessUserInfo getItemByCode(String code) {
        return successMapper.selectByCode(code);
    }

    @Override
    public boolean execKillV2(String id, Integer userId) {
        Integer killId = Integer.parseInt(id);
        //查看当前用户是否已获得秒杀商品，获得后不可秒杀
        /**
         *即生成订单数据的逻辑没有判断的逻辑快
         */
        if (successMapper.countByKillUserId(killId, userId) <= 0) {
            ItemKill itemKill = killMapper.selectById(killId);
            //判断当前商品是否能被秒杀
            if (itemKill != null && itemKill.getCanKill() == 1 && itemKill.getTotal() > 0) {
                //更新秒杀商品数返回true
                //扣减库存
                int res = killMapper.updateKillItem(killId);

                if (res > 0) {
                    //扣减成功,生成订单
                    commonRecordKillSuccess(itemKill, userId);
                }
                return true;
            }
        }
        return false;
    }

    //基于redis 分布式锁来保证共享资源

    /**
     * 基于redis 实现分布式锁。因为redis为单线程，所以即使多个线程同时发出加锁请求，但所有的命令都将保存在命令队列中，而由于redis为
     * 单线程，同一时刻只能执行一个命令，并且使用setnx 命令设置key-value值时。该命令执行时，如果redis中有该key那么将返回0并且什么都不操作
     * 如果没有该key那么返回1 并设置成功。所以同一时刻只会有一个线程设置锁成功(即拿到该锁)，但是为了保证不成为死锁，还要为该key设置一个过期时间
     * 即expire 命令，由于这两个操作不是原子操作，所以仍然有不足
     */
    @Override
    public boolean execKillV3(String id, Integer userId) {
        Integer killId = Integer.parseInt(id);
        //查看当前用户是否已获得秒杀商品，获得后不可秒杀
        /**
         *即生成订单数据的逻辑没有判断的逻辑快
         */
        //TODO: 借助redis的原子操作实现分布式锁-对共享资源进行控制
        ValueOperations operations = redisTemplate.opsForValue();
        //同一个用户只能抢一个商品 所以以用户ID和商品ID来作为key
        //那么相当于同一个用户的其它请求来抢该商品都将失败
        final String key = new StringBuffer().append(killId).append(userId).append("Redis-lock").toString();
        final String value = RandomUtil.generateOrderCode();
        //设置成功返回true 设置失败返回false
        Boolean cacheRes = operations.setIfAbsent(key, value);
         //logger.info("redis 锁设置结果: {}",cacheRes);
        //如果已经成功说明将数据插入了Redis中,那么可以进行其他的操作.即获取锁成功
        //将对共享资源的操作部分全部加锁
        if (cacheRes){
            // 只要设置成功就需要将其设置过期
            //参数为 key  时间30秒 单位未秒
            redisTemplate.expire(key, 30, TimeUnit.SECONDS);
        if (successMapper.countByKillUserId(killId, userId) <= 0) {
                try {
                    ItemKill itemKill = killMapper.selectById(killId);
                    //判断当前商品是否能被秒杀
                    if (itemKill != null && itemKill.getCanKill() == 1 && itemKill.getTotal() > 0) {
                        //更新秒杀商品数返回true
                        //扣减库存
                        int res = killMapper.updateKillItem(killId);
                        if (res > 0) {
                            //扣减成功,生成订单
                            commonRecordKillSuccess(itemKill, userId);
                        }
                        return true;
                    }
                } finally {
                    //释放获取到的锁
                    if (value.equals(operations.get(key).toString())) {
                        //将获取到的锁释放
                        redisTemplate.delete(key);
                    }
                }
            }
        }
        return false;
    }

    //使用redisson实现redis分布式锁

    /**
     * 如果项目是单机部署的话，那么只要保证该方法是线程安全的即可(使用synchronized等关键字修饰).
     * 但如果该项目是集群部署的话，即同一用户对同一商品的秒杀请求会被分摊到多个机器上处理，即使该方法使用了
     * synchronized修饰，对于单个机器来说，其是线程安全的。但仍有可能同一用户对同一商品的多个秒杀请求被多台机器同时
     * 处理，导致同一用户抢到多个相同商品。所以此时需要分布式锁来保证共享资源的线程安全。
     */
    @Override
    public boolean execKillV4(String id, Integer userId) {
        Integer killId = Integer.parseInt(id);
        /**
         * 使用redisson获取锁并同时设置锁过期时间
         */
        final String key = new StringBuffer().append(killId).append(userId).append("Redis-lock").toString();
         //其也是调用setnx 和expire命令
        RLock lock=redissonClient.getLock(key);
        //获取锁
        try {
            //尝试加锁 当前线程最多等待30秒 并且在10秒后释放该锁
            boolean casResult=lock.tryLock(30,10,TimeUnit.SECONDS);
            //对共享资源的操作进行加锁
           if(casResult){
               //查看当前用户是否已获得秒杀商品，获得后不可秒杀
               /**
                *即生成订单数据的逻辑没有判断的逻辑快
                */
               if (successMapper.countByKillUserId(killId, userId) <= 0) {
                   ItemKill itemKill = killMapper.selectById(killId);
                   //判断当前商品是否能被秒杀
                   if (itemKill != null && itemKill.getCanKill() == 1 && itemKill.getTotal() > 0) {
                       //更新秒杀商品数返回true
                       //扣减库存
                       int res = killMapper.updateKillItem(killId);
                       if (res > 0) {
                           //扣减成功,生成订单
                           commonRecordKillSuccess(itemKill, userId);
                       }
                       return true;
                   }
               }
           }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
            //强制释放锁
            //lock.forceUnlock();
        }
        return false;
    }

    /**
     *基于zookeeper分布式锁实现
     * 使用CuratorFramework框架
     *
     */
    @Override
    public boolean execKillV5(String id, Integer userId) {
        Integer killId = Integer.parseInt(id);

        /**
         * 会在pathPrefix上创建许多临时节点 如果序号是最小的那么就可以拿到锁，否则就等待
         */
        InterProcessMutex mutex=new InterProcessMutex(curatorFramework,pathPrefix+killId+userId+"-lock");
        try{
            //查看当前用户是否已获得秒杀商品，获得后不可秒杀,获取到锁进行操作
            if(mutex.acquire(10L,TimeUnit.SECONDS)){
                if (successMapper.countByKillUserId(killId, userId) <= 0) {
                    ItemKill itemKill = killMapper.selectById(killId);
                    //判断当前商品是否能被秒杀
                    if (itemKill != null && itemKill.getCanKill() == 1 && itemKill.getTotal() > 0) {
                        //更新秒杀商品数返回true
                        //扣减库存
                        int res = killMapper.updateKillItem(killId);
                        if (res > 0) {
                            //扣减成功,生成订单
                            commonRecordKillSuccess(itemKill, userId);
                        }
                        return true;
                    }
                }
            }
        }catch (Exception e){
         logger.error("获取Zookeeper锁失败");
        }
        finally {
          if(mutex!=null){
              try {
                  //释放锁
                  mutex.release();
              } catch (Exception e) {
                  e.printStackTrace();
              }
          }
        }
        return false;
    }

    //生成用户订单
    private void commonRecordKillSuccess(ItemKill kill, Integer userId) {
        String codeNo = String.valueOf(flake.nextId());
        //生成订单信息存入数据库中
        ItemKillSuccess itemKillSuccess = new ItemKillSuccess();
        //设置秒杀成功订单编号
        itemKillSuccess.setCode(codeNo);
        //商品ID
        itemKillSuccess.setItemId(kill.getItemId());

        itemKillSuccess.setKillId(kill.getId());
        itemKillSuccess.setCreateTime(new Date());
        //秒杀成功的用户ID
        itemKillSuccess.setUserId(String.valueOf(userId));

        itemKillSuccess.setStatus((byte) 0);

        //TODO:在此处进行双重校验 在查一次数据库 查看是否有当前用户的购买记录，实际上就是为一开始的判断争取时间
        //TODO：多线程环境下，可能来不及写入数据库就已经响应了另外一个请求，所以相当于在此处再进行一次查询
        //TODO：但是会影响数据库性能
        if (successMapper.countByKillUserId(kill.getId(), userId) <= 0) {
            //如果订单插入成功
            int res = successMapper.insert(itemKillSuccess);
            if (res > 0) {
                //异步邮件消息通知 rabbitmq+mail
                //将订单编号发送到rabbitmq中
                rabbitmqSenderService.sendKillSuccessEmailMsg(codeNo);
            }
            //将该订单信息放入死信队列中，在超过一定时间之后仍未支付(即仍未被消费)，则将该订单信息置为已失效
            rabbitmqSenderService.sendKillSuccessOrderExpirMsg(codeNo);
        }
    }


}
