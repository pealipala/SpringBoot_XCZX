package com.xuecheng.order.mq;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.order.config.RabbitMQConfig;
import com.xuecheng.order.service.TaskService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class ChooseCourseTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChooseCourseTask.class);
    @Autowired
    private TaskService taskService;
    
    /**
     * 每隔1分钟扫描消息表，向mq发送消息
     * @author : yechaoze
     * @date : 2019/8/1 13:00
     * @return : void
     */
    @Scheduled(cron = "0/3 * * * * *")
    public void sendChooseCourseTask(){
        Calendar calendar=new GregorianCalendar();
        calendar.setTime(new Date());
        //取出一分钟前的时间
        calendar.add(GregorianCalendar.MINUTE,-1);
        Date time = calendar.getTime();
        List<XcTask> list = taskService.findTaskList(time, 100);
        for (XcTask xcList:list){
            if (taskService.getTask(xcList.getId(),xcList.getVersion())>0){
                taskService.publish(xcList,xcList.getMqExchange(),xcList.getMqRoutingkey());
            }
        }
    }

    /**
     * 监听已完成添加选课队列 执行当前任务和历史任务的删除和插入
     * @author : yechaoze
     * @date : 2019/8/1 18:06
     * @param task :
     * @return : void
     */
    @RabbitListener(queues = {RabbitMQConfig.XC_LEARNING_FINISHADDCHOOSECOURSE})
    public void receiveFinishChooseCourseTask(XcTask task) throws IOException {
        String id = task.getId();
        if (task!=null&& StringUtils.isNotEmpty(id))
            //删除任务，添加历史任务
        taskService.finishTask(id);
    }
}
