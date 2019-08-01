package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginFilterTest extends ZuulFilter {

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
        return 2;
    }

    @Override
    public boolean shouldFilter() {
        //返回true表示要执行此过滤
        return false;
    }

    //过滤器的内容
    //过滤所有请求，判断头部信息是否有Authorization，如果没有则拒绝访问，否则转发到微服务。
    @Override
    public Object run()  {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        HttpServletResponse response = requestContext.getResponse();
        String header = request.getHeader("Authorization");
        if (StringUtils.isEmpty(header)){
            requestContext.setSendZuulResponse(false);// 拒绝访问
            requestContext.setResponseStatusCode(200);// 设置响应状态码
            ResponseResult unauthenticated = new ResponseResult(CommonCode.FAIL);
            String jsonString = JSON.toJSONString(unauthenticated);
            requestContext.setResponseBody(jsonString);
            response.setContentType("application/json;charset=UTF‐8");
            return null;
        }
        return null;
    }
}
