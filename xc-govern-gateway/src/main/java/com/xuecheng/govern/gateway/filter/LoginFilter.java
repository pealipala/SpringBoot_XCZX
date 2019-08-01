package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 身份校验过滤器
 * @author : yechaoze
 * @date : 2019/7/27 21:26
 */
@Component
public class LoginFilter extends ZuulFilter {

    @Autowired
    private AuthService authService;

    //过滤器的类型
    @Override
    public String filterType() {
        /**
         pre：请求在被路由之前执行
         routing：在路由请求时调用
         post：在routing和errror过滤器之后调用
         error：处理请求时发生错误调用
         */
        return "pre";
    }

    //数字越小越先执行
    @Override
    public int filterOrder() {
        return 0;
    }

    @Override
    public boolean shouldFilter() {
        //返回true表示要执行此过滤
        return true;
    }

    //过滤器的内容
    //过滤所有请求，判断头部信息是否有Authorization，如果没有则拒绝访问，否则转发到微服务。
    @Override
    public Object run()  {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();

        //取cookie中的身份令牌
        String token = authService.getTokenFromCookie(request);
        if (StringUtils.isEmpty(token)){
            //拒绝访问
            this.access_denied();
            return null;
        }
        //取头信息中的jwt令牌
        String jwt = authService.getJwtFromHeader(request);
        if (StringUtils.isEmpty(jwt)){
            //拒绝访问
            this.access_denied();
            return null;
        }

        //取redis中取jwt过期时间
        long time = authService.getJwtFromRedis(token);
        if (time<0){
            //拒绝访问
            this.access_denied();
            return null;
        }

        return null;
    }

    /**
     * 无权限下拒绝访问
     * @author : yechaoze
     * @date : 2019/7/28 10:42
     * @return : void
     */
    private void access_denied(){
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletResponse response = requestContext.getResponse();
        requestContext.setSendZuulResponse(false);// 拒绝访问
        requestContext.setResponseStatusCode(200);// 设置响应状态码
        ResponseResult unauthenticated = new ResponseResult(CommonCode.FAIL);
        String jsonString = JSON.toJSONString(unauthenticated);
        requestContext.setResponseBody(jsonString);
        response.setContentType("application/json;charset=UTF‐8");
    }


}
