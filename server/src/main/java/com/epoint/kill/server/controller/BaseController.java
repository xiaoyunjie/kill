package com.epoint.kill.server.controller;

import com.epoint.kill.api.reponse.ResponseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/test")
public class BaseController {

    @RequestMapping(value = "/hello.do")
    public String test(){
        return "hello world";
    }

    @RequestMapping("/json.do")
    public ResponseResult<List<String>> getJson(){
        ResponseResult<List<String>> rr=new ResponseResult<>();
        rr.setCode(200);
        rr.setMsg("成功");
      List<String> list=new ArrayList<>();
      list.add("emmm");
      rr.setData(list);
      return rr;
    }

}
