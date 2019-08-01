package com.xuecheng.auth.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Value("${auth.tokenValiditySeconds}")
    private long ttl;

    /**
     * 用户认证获取令牌 并 存储到redis
     * @author : yechaoze
     * @date : 2019/7/25 12:28
     * @param username :
     * @param password :
     * @param clientId :
     * @param clientSecret :
     * @return : com.xuecheng.framework.domain.ucenter.ext.AuthToken
     */
    public AuthToken login(String username, String password, String clientId, String clientSecret) {
        //请求SpringSecurity获取了令牌
        AuthToken authToken = this.getAuthToken(username, password, clientId, clientSecret);
        if (authToken==null){
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_APPLYTOKEN_FAIL);
        }
        //将token存储到redis
        //令牌
        String access_token = authToken.getAccess_token();
        //内容
        String content = JSON.toJSONString(authToken);
        boolean token = this.saveToken(access_token, content, ttl);
        if (!token){
            ExceptionCast.cast(AuthCode.AUTH_LOGIN_TOKEN_SAVEFAIL);
        }
        return authToken;
    }

    /**
     * 将token存储到redis
     * @author : yechaoze
     * @date : 2019/7/25 13:21
     * @param access_token :令牌
     * @param content : AuthToken对象的内容
     * @param ttl :过期时间
     * @return : boolean
     */
    private boolean saveToken(String access_token,String content,long ttl){
        String key="user_token:"+access_token;
        redisTemplate.boundValueOps(key).set(content,ttl,TimeUnit.SECONDS);
        Long expire = redisTemplate.getExpire(key,TimeUnit.SECONDS);
        return expire>0;
    }

    /**
     * 获取令牌
     * @author : yechaoze
     * @date : 2019/7/25 13:06
     * @param username :
     * @param password :
     * @param clientId :
     * @param clientSecret :
     * @return : java.util.Map
     */
    private AuthToken getAuthToken(String username, String password, String clientId, String clientSecret){
        //远程调用获取uri地址
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        URI uri = serviceInstance.getUri();
        String authUrl=uri+"/auth/oauth/token";

        //设置headers
        LinkedMultiValueMap<String,String> headers=new LinkedMultiValueMap<>();
        String httpBasic = this.getHttpBasic(clientId, clientSecret);
        headers.add("Authorization", httpBasic);

        //设置body
        LinkedMultiValueMap<String,String> body=new LinkedMultiValueMap<>();
        body.add("grant_type","password");
        body.add("username",username);
        body.add("password",password);

        HttpEntity<MultiValueMap<String, String>> httpEntity=new HttpEntity<>(body,headers);


        //指定 restTemplate当遇到400或401响应时候也不要抛出异常，也要正常返回值
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                if (response.getRawStatusCode() != 400 && response.getRawStatusCode() != 401) {
                    super.handleError(response);
                }
            }
        });
        ResponseEntity<Map> exchange = restTemplate.exchange(authUrl, HttpMethod.POST, httpEntity, Map.class);
        //申请令牌信息
        Map bodyMap = exchange.getBody();
        if (bodyMap == null ||
                bodyMap.get("access_token") == null ||
                bodyMap.get("refresh_token") == null ||
                bodyMap.get("jti") == null) {

            //解析spring security返回的错误信息
            if (bodyMap != null && bodyMap.get("error_description") != null) {
                String error_description = (String) bodyMap.get("error_description");
                if (error_description.contains("UserDetailsService returned null")) {
                    ExceptionCast.cast(AuthCode.AUTH_ACCOUNT_NOTEXISTS);
                } else if (error_description.contains("坏的凭证")) {
                    ExceptionCast.cast(AuthCode.AUTH_CREDENTIAL_ERROR);
                }
            }
            return null;
        }
        AuthToken authToken=new AuthToken();
        authToken.setAccess_token((String) bodyMap.get("jti"));//用户身份令牌
        authToken.setJwt_token((String) bodyMap.get("access_token"));//jwt令牌
        authToken.setRefresh_token((String) bodyMap.get("refresh_token"));//刷新令牌
        return authToken;
    }

    /**
     * 获取httpBasic
     * @author : yechaoze
     * @date : 2019/7/25 12:31
     * @param clientId :
     * @param clientSecret :
     * @return : java.lang.String 返回格式 Basic xxxxxxxxxxxxxxxxxx
     */
    private String getHttpBasic(String clientId, String clientSecret){
        String string=clientId+":"+clientSecret;
        byte[] bytes = Base64Utils.encode(string.getBytes());
        return "Basic "+new String(bytes);
    }

    /**
     * 从redis中取令牌
     * @author : yechaoze
     * @date : 2019/7/27 17:21
     * @param token :
     * @return : com.xuecheng.framework.domain.ucenter.ext.AuthToken
     */
    public AuthToken getJwtFromRedis(String token){
        String key="user_token:"+token;
        //redis中取到的数据
        String value = redisTemplate.opsForValue().get(key);
        try {
            AuthToken authToken = JSON.parseObject(value, AuthToken.class);
            return authToken;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * 删除redis中的token
     * @author : yechaoze
     * @date : 2019/7/27 19:54
     * @param access_token :
     * @return : boolean
     */
    public boolean delToken(String access_token) {
        String key="user_token:"+access_token;
        redisTemplate.delete(key);
        return true;
    }
}
