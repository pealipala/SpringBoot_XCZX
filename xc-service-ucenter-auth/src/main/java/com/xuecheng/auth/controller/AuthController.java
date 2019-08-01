package com.xuecheng.auth.controller;

import com.alibaba.fastjson.JSON;
import com.xuecheng.api.auth.AuthControllerApi;
import com.xuecheng.auth.service.AuthService;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping("/")
public class AuthController implements AuthControllerApi {

    @Autowired
    private AuthService authService;
    @Value("${auth.clientId}")
    private String clientId;
    @Value("${auth.clientSecret}")
    private String clientSecret;
    @Value("${auth.cookieDomain}")
    private String domain;
    @Value("${auth.cookieMaxAge}")
    private int cookieMaxAge;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    @PostMapping("/userLogin")
    public LoginResult login(LoginRequest loginRequest) {
        if (loginRequest==null|| StringUtils.isEmpty(loginRequest.getUsername())){
            ExceptionCast.cast(AuthCode.AUTH_USERNAME_NONE);
        }
        if (loginRequest==null|| StringUtils.isEmpty(loginRequest.getPassword())){
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        AuthToken authToken= authService.login(username,password,clientId,clientSecret);
        //将令牌存储到cookie
        String access_token = authToken.getAccess_token();
        this.saveCookie(access_token);
        return new LoginResult(CommonCode.SUCCESS,access_token);
    }


    /**
     * 存储cookie
     * @author : yechaoze
     * @date : 2019/7/25 13:51
     * @param token :
     * @return : void
     */
    private void saveCookie(String token){
        //HttpServletResponse response,String domain,String path,String name,String value,int maxAge,boolean httpOnly
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        CookieUtil.addCookie(response,domain,"/","uid",token,cookieMaxAge,false);
    }

    @Override
    @PostMapping("/userLogout")
    public ResponseResult logout() {
        //从cookie中取用户身份令牌
        String token = this.getTokenFromCookie();
        if (token==null){
            return new JwtResult(CommonCode.FAIL,null);
        }
        //将redis中的token删除
        boolean delToken = authService.delToken(token);
        //将cookie中的token删除
        this.clearCookie(token);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    @Override
    @GetMapping("/userJwt")
    public JwtResult userJwt() {
        //从cookie中取用户身份令牌
        String token = this.getTokenFromCookie();
        if (token==null){
            return new JwtResult(CommonCode.FAIL,null);
        }

        //拿身份令牌查询jwt令牌
        AuthToken authToken = authService.getJwtFromRedis(token);
        if (authToken!=null){

            //将jwt令牌返回给用户
            String jwt_token = authToken.getJwt_token();
            return new JwtResult(CommonCode.SUCCESS,jwt_token);
        }
        return null;
    }


    /**
     * 从cookie中取用户身份令牌
     * @author : yechaoze
     * @date : 2019/7/27 16:59
     * @return : java.lang.String
     */
    private String getTokenFromCookie(){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> map = CookieUtil.readCookie(request, "uid");
        if (map!=null&&map.get("uid")!=null){
            String uid = map.get("uid");
            return uid;
        }
        return null;
    }

    /**
     * 从cookie中清楚token
     * @author : yechaoze
     * @date : 2019/7/27 19:59
     * @return : void
     */
    private void clearCookie(String token){
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        CookieUtil.addCookie(response,domain,"/","uid",token,0,false);
    }


}
