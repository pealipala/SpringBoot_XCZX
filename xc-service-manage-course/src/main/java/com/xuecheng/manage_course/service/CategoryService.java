package com.xuecheng.manage_course.service;

import com.xuecheng.framework.domain.course.ext.CategoryNode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.manage_course.dao.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 课程分类
 * @author : yechaoze
 * @date : 2019/6/20 10:38
 */
@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    public CategoryNode findList(){
        CategoryNode categoryNode = categoryMapper.selectList();
        if (categoryNode==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        return categoryNode;
    }
}
