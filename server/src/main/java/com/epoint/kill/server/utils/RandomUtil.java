package com.epoint.kill.server.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 生成订单编号
 * 时间戳加4位随机数
 */
public class RandomUtil {

    //日期处理类
    private static final SimpleDateFormat format=new SimpleDateFormat("yyyyMMddHHmmssSS");

    //在并发程序中使用ThreadLocalRandom而不是共享的Random对象通常会遇到更少的开销和争用
    private static final ThreadLocalRandom random=ThreadLocalRandom.current();


    //生成订单编号
    public static String generateOrderCode (){
        //以当前时间戳和4位随机数生成一个订单编号
        return format.format(new Date())+genericNumber(4);
    }
    //生成随机数
    public static String genericNumber(final int num){
   //buffer 线程安全
   StringBuffer stringBuffer=new StringBuffer();
        for(int i=0;i<num;i++){
            //在0到9之间选一个随机数生成
            //返回0（包括）和指定的绑定（排除）之间的伪随机 int值。
            stringBuffer.append(random.nextInt(9));
        }
        return stringBuffer.toString();
    }

    }
