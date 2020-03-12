package com.epoint.kill.model.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MailDto {

    //主题
    private String subject;

    //内容
    private String content;

    //接收人
    private String[] tos;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String[] getTos() {
        return tos;
    }

    public void setTos(String[] tos) {
        this.tos = tos;
    }
}
