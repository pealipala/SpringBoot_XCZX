package com.xuecheng.order.service;

import com.xuecheng.framework.domain.task.XcTask;
import com.xuecheng.framework.domain.task.XcTaskHis;
import com.xuecheng.order.dao.XcTaskHisRepository;
import com.xuecheng.order.dao.XcTaskRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    @Autowired
    private XcTaskRepository xcTaskRepository;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private XcTaskHisRepository xcTaskHisRepository;

    /**
     * 查询一分钟前任务列表
     * @author : yechaoze
     * @date : 2019/8/1 12:14
     * @param updateTime :
     * @param size :
     * @return : java.util.List<com.xuecheng.framework.domain.task.XcTask>
     */
    public List<XcTask> findTaskList(Date updateTime,int size){
        Pageable pageable=new PageRequest(0,size);
        Page<XcTask> xcTasks = xcTaskRepository.findByUpdateTimeBefore(pageable, updateTime);
        List<XcTask> list = xcTasks.getContent();
        return list;
    }


    /**
     * 发布消息
     * @author : yechaoze
     * @date : 2019/8/1 14:08
     * @param xcTask :
     * @param ex :
     * @param routingKey :
     * @return : void
     */
    @Transactional
    public void publish(XcTask xcTask,String ex,String routingKey){
        Optional<XcTask> optional = xcTaskRepository.findById(xcTask.getId());
        if (optional.isPresent()){
            rabbitTemplate.convertAndSend(ex,routingKey,xcTask);
            //更新任务时间
            XcTask one = optional.get();
            one.setUpdateTime(new Date());
            xcTaskRepository.save(one);
        }
    }

    /**
     * 取任务
     * @author : yechaoze
     * @date : 2019/8/1 15:14
     * @param id :
     * @param version :
     * @return : int
     */
    @Transactional
    public int getTask(String id,int version){
        int result = xcTaskRepository.updateTaskVersion(id, version);
        return result;
    }


    /**
     * 完成任务
     * @author : yechaoze
     * @date : 2019/8/1 17:44
     * @param taskId :
     * @return : void
     */
    @Transactional
    public void finishTask(String taskId){
        Optional<XcTask> optional = xcTaskRepository.findById(taskId);
        if (optional.isPresent()){
            //当前任务
            XcTask xcTask = optional.get();
            //历史任务
            XcTaskHis xcTaskHis=new XcTaskHis();
            BeanUtils.copyProperties(xcTask,xcTaskHis);
            xcTaskHisRepository.save(xcTaskHis);
            xcTaskRepository.delete(xcTask);
        }
    }

}
