package com.xuecheng.govern.gateway.service;

import com.xuecheng.framework.utils.CookieUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 从头取jwt令牌
     * @author : yechaoze
     * @date : 2019/7/28 10:26
     * @param request : 
     * @return : java.lang.String
     */
    public String getJwtFromHeader(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        if (StringUtils.isEmpty(authorization)){
            return null;
        }
        boolean startsWith = authorization.startsWith("Bearer ");
        if (!startsWith){
            return null;
        }
        String jwt = authorization.substring(7);
        return jwt;
    }

    /**
     * redis中查询令牌有效期
     * @author : yechaoze
     * @date : 2019/7/28 10:36
     * @param access_token :
     * @return : long
     */
    public long getJwtFromRedis(String access_token){
        String key="user_token:"+access_token;
        Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        return expire;
    }

    /**
     * 从cookie中取token
     * @author : yechaoze
     * @date : 2019/7/28 10:30
     * @param request : 
     * @return : java.lang.String
     */
    public String getTokenFromCookie(HttpServletRequest request){
        Map<String, String> uid = CookieUtil.readCookie(request, "uid");
        if (uid==null){
            return null;
        }
        String access_token = uid.get("uid");
        if (access_token!=null){
            return access_token;
        }
        return null;
    }
}
