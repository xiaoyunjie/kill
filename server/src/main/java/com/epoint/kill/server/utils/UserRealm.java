package com.epoint.kill.server.utils;

import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

public class UserRealm extends AuthorizingRealm {
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        return null;
    }

    @Override
    //登录认证
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        // 获取用户token
        UsernamePasswordToken token=(UsernamePasswordToken)authenticationToken;
        String username=token.getUsername();
        //获取数据库用户名
        if(username.equals("")||!username.equals("admin")){
            //如果用户名不存在那么只要返回一个null即可 其会自动判断
            return null;
        }
        //第二个为需要匹配的密码
        return new SimpleAuthenticationInfo("","Epoint@123","");
    }
    }
