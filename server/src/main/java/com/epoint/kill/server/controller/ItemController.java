package com.epoint.kill.server.controller;

import com.epoint.kill.api.reponse.ResponseResult;
import com.epoint.kill.model.dto.KillSuccessUserInfo;
import com.epoint.kill.model.entity.Item;
import com.epoint.kill.model.entity.ItemKill;
import com.epoint.kill.model.entity.ItemKillSuccess;
import com.epoint.kill.server.service.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@RestController
@RequestMapping("/item")
public class ItemController {

    private static final Logger logger= LoggerFactory.getLogger(ItemController.class);

    //请求前缀
    private static final String priex="item";

    @Autowired
    ItemService itemService;


    //接受全部请求返回
    @RequestMapping(value = "/",method = RequestMethod.GET)
    public String getIndex(){
       //获取秒杀商品并返回

        return "list";
    }
    @RequestMapping("/getAll.do")
    public ResponseResult<List<ItemKill>> getAll(){
        ResponseResult<List<ItemKill>> rr=new ResponseResult<>();
        rr.setCode(200);
        rr.setMsg("响应成功");
        List<ItemKill> items=itemService.getItemKills();
        Iterator<ItemKill> iterator=items.iterator();
        while (iterator.hasNext()){
            if(iterator.next().getCanKill()==0){
                iterator.remove();
            }
        }
        rr.setCount(items.size());
        rr.setData(items);
        return rr;
    }

    @RequestMapping("/execKill.do")
    public ResponseResult<Void> execKill(@RequestParam("id") String id){
        ResponseResult<Void> rr=new ResponseResult<>();
        //用户登录时将用户ID 存入session中
        boolean result=itemService.execKillV3(id,10);
        if(result){
            rr.setMsg("秒杀成功,请您登录邮箱支付订单");
        }else{
            rr.setCode(300);
            rr.setMsg("您已秒杀过该商品,秒杀失败");
        }
        return rr;
    }

    /**
     * get请求的参数会拼接在URL 后面以？分隔 ，发起Get请求时，浏览器用application/x-www-form-urlencoded(contentType 参数值)方式，将表单数据转换成一个字符串(key1=value1&key2=value2...)拼接到url上
     *
     * post请求其数据保存在http entity中，但是其未规定post请求的数据解析方式 一共有四种解析方式。默认的为application/x-www-form-urlencoded
     * 即也是k=v格式数据并保存在http entity中。在服务端获取时可以使用@RequestParam 注解从请求中拿取对应的参数（get请求，post请求都可以获取数据。只要解析方式为application/x-www-form-urlencoded（表单格式）即可）。
     * 但是注意如果数据格式为application/json时，那么必须要使用@RequestBody注解获取
     *
     * 所以说@RequestParam 注解可以获取请求数据解析格式为application/x-www-form-urlencoded的参数值 不一定非要是get请求或者post请求
     https://juejin.im/post/5b5efff0e51d45198469acea
     https://segmentfault.com/a/1190000014343759
     */

    @RequestMapping("/execLock.do")
    public ResponseResult<Void> execKillLock(@RequestParam("id") String id,@RequestParam("userId") String userId){
        logger.info("请求数据为 {}",userId);
        ResponseResult<Void> rr=new ResponseResult<>();
        //用户登录时将用户ID 存入session中
        try{
            //在不加分布式锁的情况下
            /**
             * 未控制多线程对临界资源的访问
             * 即未控制共享资源的访问
             */
            /**
             * 解决方案: 分布式锁解决共享资源在高并发访问情况下出现的并发安全的问题
             * 协助方案: 对于瞬时流量、并发请求进行限流(目前是对其接口进行限流,也可以对于网关进行限流)
             */
            boolean result=itemService.execKillV4(id,Integer.parseInt(userId));
            if(result){
                rr.setMsg("秒杀成功,请您登录邮箱支付订单");
            }else{
                rr.setCode(300);
                rr.setMsg("您已秒杀过该商品,秒杀失败");
            }
        }catch (Exception e){
           rr.setCode(500);
           rr.setMsg(e.getMessage());
        }
        return rr;
    }

    //获取商品详情
    //@RequestBody 用于标注在实体类上获取post请求传输的数据
    @RequestMapping("/getItem.do")
    public ResponseResult<Item> getItem(@RequestParam(value = "id",required = true) String id){


        ResponseResult<Item> rr=new ResponseResult<>();
        rr.setData(itemService.getItemById(id));
        return rr;
    }

    @RequestMapping(value = "/detail.do",method = RequestMethod.GET)
    public ResponseResult<KillSuccessUserInfo> getItemDetail(@RequestParam("code") String code){
            //根据订单编号查询订单详情
        ResponseResult<KillSuccessUserInfo> rr=new ResponseResult<>();
        rr.setCode(200);
        rr.setMsg("查询成功");
        rr.setData(itemService.getItemByCode(code));
        return rr;
    }




}
