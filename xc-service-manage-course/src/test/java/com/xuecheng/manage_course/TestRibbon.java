package com.xuecheng.manage_course;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestRibbon {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void testRibbon(){
        String serverId="XC-SERVICE-MANAGE-CMS";
        ResponseEntity<Map> forEntity = restTemplate.getForEntity("http://" + serverId + "/cms/page/get/5a754adf6abb500ad05688d9", Map.class);
        Map body = forEntity.getBody();
        System.out.println(body);

    }

}
