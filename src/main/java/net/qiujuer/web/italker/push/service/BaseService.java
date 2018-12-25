package net.qiujuer.web.italker.push.service;

import net.qiujuer.web.italker.push.bean.db.User;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

public class BaseService {
    //添加一个上下文注解，该注解会自动赋值，具体为拦截器返回的SecurityContext
    @Context
    protected SecurityContext securityContext;

    /*
     * 从上下文获取自己
     */
    protected User getSelf() {
        return (User) securityContext.getUserPrincipal();
    }
}
