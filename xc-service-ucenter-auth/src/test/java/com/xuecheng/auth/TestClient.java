package com.xuecheng.auth;

import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Base64Utils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestClient {

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Test
    public void testClient(){
        //远程调用获取uri , 从eureka获取认证服务的ip和端口
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        URI uri = serviceInstance.getUri();
        //拼接url
        String url= uri+"/auth/oauth/token";
        //请求内容
        //1、header信息,包括了http basic认证信息
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        String httpbasic = this.getHttpBasic("XcWebApp", "XcWebApp");
        headers.add("Authorization", httpbasic);
        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
        //2、body信息
        body.add("grant_type","password");
        body.add("username","itcast");
        body.add("password","123");
        HttpEntity<MultiValueMap<String, String>> HttpEntity = new HttpEntity<>( body, headers);

        //指定 restTemplate当遇到400或401响应时候也不要抛出异常，也要正常返回值
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                //当响应的值为400或401时候也要正常响应，不要抛出异常
                if(response.getRawStatusCode()!=400 && response.getRawStatusCode()!=401){
                    super.handleError(response);
                }
            }
        });
        ResponseEntity<Map> exchange = restTemplate.exchange(url, HttpMethod.POST, HttpEntity, Map.class);
        //申请令牌的信息
        Map bodys = exchange.getBody();
        System.out.println(bodys);
    }

    private String getHttpBasic(String id,String password){
        String string=id+":"+password;
        byte[] bytes = Base64Utils.encode(string.getBytes());
        return "Basic "+new String(bytes);
    }

    @Test
    public void getAuthToken(){
        //远程调用获取uri地址
        ServiceInstance serviceInstance = loadBalancerClient.choose(XcServiceList.XC_SERVICE_UCENTER_AUTH);
        URI uri = serviceInstance.getUri();
        String authUrl=uri+"/auth/oauth/token";

        //设置headers
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<String, String>();
        String httpBasic = this.getHttpBasic("XcWebApp", "XcWebApp");
        headers.add("Authorization", httpBasic);

        //设置body
        MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
        body.add("grant_type","password");
        body.add("username","itcast");
        body.add("password","123");

        HttpEntity<MultiValueMap<String, String>> httpEntity=new HttpEntity<>(headers,body);
        ResponseEntity<Map> exchange = restTemplate.exchange(authUrl, HttpMethod.POST, httpEntity, Map.class);

        //指定 restTemplate当遇到400或401响应时候也不要抛出异常，也要正常返回值
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                //当响应的值为400或401时候也要正常响应，不要抛出异常
                if(response.getRawStatusCode()!=400 && response.getRawStatusCode()!=401){
                    super.handleError(response);
                }
            }
        });
        Map jwtBody = exchange.getBody();
        System.out.println(jwtBody);
    }




}
