package com.epoint.kill.server.service;

import com.epoint.kill.model.dto.MailDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;

@Service
@EnableAsync
//@EnableAsync开启异步执行的功能，这可以放在springboot主类上，或者放在我们的配置类上。
public class MailService {
    private final  static Logger logger = LoggerFactory.getLogger(RabbitmqReciveService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    Environment environment;
    /**
     * 发送简单文本文件
     * 开始一个异步任务 其会新开一个线程来执行该方法
     * 异步任务，主线程不会等待当前程序的执行结果而直接返回
     */
    @Async
    public void sendSimpleEmail(final MailDto mailDto){
        try{
            SimpleMailMessage message=new SimpleMailMessage();
            message.setFrom(environment.getProperty("mail.send.from"));
            //设置收件人
            message.setTo(mailDto.getTos());
            message.setSubject(mailDto.getSubject());
            message.setText(mailDto.getContent());
            mailSender.send(message);
            logger.info("-文本发送成功-");
        }catch (Exception e){
           logger.error("-文本发送失败-");
        }
    }

    //发送HTML 邮件
    //注意Async 标注的方法只能是void或者异步调用返回值
    @Async
    public void sendHTMLmail(final MailDto mailDto){
        try{
            MimeMessage message=mailSender.createMimeMessage();
            MimeMessageHelper messageHelper=new MimeMessageHelper(message,true,"utf-8");
            messageHelper.setFrom(environment.getProperty("mail.send.from"));
            messageHelper.setSubject(mailDto.getSubject());
            messageHelper.setTo(mailDto.getTos());
            messageHelper.setText(mailDto.getContent(),true);
            mailSender.send(message);
            logger.info("发送邮件成功");
        }catch (Exception e){
          logger.error("发送邮件失败");
        }
    }
}
