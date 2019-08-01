package com.xuecheng.learning.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.learning.config.RabbitMQConfig;
import com.xuecheng.learning.service.LearningService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class ChooseCourseTask {
    @Autowired
    private LearningService learningService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    /**
     * 接收选课任务
     */
    @RabbitListener(queues = {RabbitMQConfig.XC_LEARNING_ADDCHOOSECOURSE})
    public void receiveChooseCourseTask(XcTask xcTask) throws IOException {

        //取出消息的内容
        String requestBody = xcTask.getRequestBody();
        Map map = JSON.parseObject(requestBody, Map.class);
        String userId = (String) map.get("userId");
        String courseId = (String) map.get("courseId");
        //解析出valid, Date startTime, Date endTime...

        //添加选课
        //String userId, String courseId, String valid, Date startTime, Date endTime, XcTask xcTask
        ResponseResult addCourse = learningService.addCourse(userId, courseId, null, null, null, xcTask);
        if(addCourse.isSuccess()){
            //添加选课成功，要向mq发送完成添加选课的消息
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_LEARNING_ADDCHOOSECOURSE,RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE_KEY,xcTask);
        }
    }

}
