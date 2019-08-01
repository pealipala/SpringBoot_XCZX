package com.xuecheng.auth;

import com.alibaba.fastjson.JSON;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;
import org.springframework.test.context.junit4.SpringRunner;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestJwt {

    /**
     * 创建令牌
     * @author : yechaoze
     * @date : 2019/7/25 0:06
     * @return : void
     */
    @Test
    public void testCreateJwt(){
        //证书文件
        String key_location = "xc.keystore";
        //密钥库密码
        String keystore_password = "xuechengkeystore";
        //访问证书路径
        ClassPathResource resource = new ClassPathResource(key_location);
        //密钥工厂
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(resource, keystore_password.toCharArray());
        //密钥的密码，此密码和别名要匹配
        String keypassword = "xuecheng";
        //密钥别名
        String alias = "xckey";
        //密钥对（密钥和公钥）
        KeyPair keyPair = keyStoreKeyFactory.getKeyPair(alias,keypassword.toCharArray());
        //私钥
        RSAPrivateKey aPrivate = (RSAPrivateKey) keyPair.getPrivate();
        //定义payload信息
        Map<String, Object> tokenMap = new HashMap<>();
        tokenMap.put("id", "123");
        tokenMap.put("name", "mrt");
        tokenMap.put("roles", "r01,r02");
        tokenMap.put("ext", "1");
        //生成jwt令牌
        Jwt jwt = JwtHelper.encode(JSON.toJSONString(tokenMap), new RsaSigner(aPrivate));
        //取出jwt令牌
        String token = jwt.getEncoded();
        System.out.println("token="+token);
    }

    /**
     * 校验
     * @author : yechaoze
     * @date : 2019/7/25 0:42
     * @return : void
     */
    @Test
    public void testVerify(){
        //jwt令牌
        String token ="eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJjb21wYW55SWQiOiIxIiwidXNlcnBpYyI6bnVsbCwidXNlcl9uYW1lIjoiaXRjYXN0Iiwic2NvcGUiOlsiYXBwIl0sIm5hbWUiOiJ0ZXN0MDIiLCJ1dHlwZSI6IjEwMTAwMiIsImlkIjoiNDkiLCJleHAiOjE1NjQ0NTE0MjIsImp0aSI6IjNjMWI4YTRkLWQ3ZWMtNDI5ZC1hNmE3LWM4OWQzOTliMjlmNiIsImNsaWVudF9pZCI6IlhjV2ViQXBwIn0.AsYqEE0MbnldsHyjuSVbAXMnY-EF2O8fmWLnaMYuzWzIscoMdp1HWLdEPjB1OGPF2pdM0dLPu5zgOBVy4R38qNzmwNBIceWj6OY4FP_ajKJ8OqKkS4_Ja8E3KGTy-TqqbxhWGfQNNP82WE6HoMSCP2aJvDCmou0HA3Yd3XCB0pkt9RWd2CshezWGySys0se9K_BAlmybQ8LiMGqky_te_z7DqFsz9FIEmSQMWU9DNFmGuz3AgAiHwpBqVfb5ji5DaN5aoo1KPdpbRRzzSy0iG1Nm56yLC7m32fLJirj_RrH4GKlgbn7ZXtErECCgxCW7fnBJ51XZRoM3VBT-sl4FCQ";
        //公钥
        String publickey = "-----BEGIN PUBLIC KEY-----MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnASXh9oSvLRLxk901HANYM6KcYMzX8vFPnH/To2R+SrUVw1O9rEX6m1+rIaMzrEKPm12qPjVq3HMXDbRdUaJEXsB7NgGrAhepYAdJnYMizdltLdGsbfyjITUCOvzZ/QgM1M4INPMD+Ce859xse06jnOkCUzinZmasxrmgNV3Db1GtpyHIiGVUY0lSO1Frr9m5dpemylaT0BV3UwTQWVW9ljm6yR3dBncOdDENumT5tGbaDVyClV0FEB1XdSKd7VjiDCDbUAUbDTG1fm3K9sx7kO1uMGElbXLgMfboJ963HEJcU01km7BmFntqI5liyKheX+HBUCD4zbYNPw236U+7QIDAQAB-----END PUBLIC KEY-----";
        //校验jwt
        Jwt jwt = JwtHelper.decodeAndVerify(token, new RsaVerifier(publickey));
        //获取jwt原始内容
        String claims = jwt.getClaims();
        //jwt令牌
        String encoded = jwt.getEncoded();
        System.out.println(encoded);
        System.out.println(claims);
    }



}
