package com.xuecheng.learning.dao;

import com.xuecheng.framework.domain.learning.XcLearningCourse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface XcLearningCourseRepository extends JpaRepository<XcLearningCourse,String> {
    //根据用户id 和 课程id 查询
    XcLearningCourse findByUserIdAndCourseId(String userId,String courseId);
}
