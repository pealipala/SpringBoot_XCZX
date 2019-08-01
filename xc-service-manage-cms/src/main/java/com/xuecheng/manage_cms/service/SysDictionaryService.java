package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.system.SysDictionary;

import com.xuecheng.manage_cms.dao.SysDictionaryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 数据字典的查询
 * @author : yechaoze
 * @date : 2019/6/20 21:41
 */
@Service
public class SysDictionaryService {

    @Autowired
    private SysDictionaryRepository sysDictionaryRepository;

    public SysDictionary findDictionaryByType(String type){
        return sysDictionaryRepository.findByDType(type);
    }
}
