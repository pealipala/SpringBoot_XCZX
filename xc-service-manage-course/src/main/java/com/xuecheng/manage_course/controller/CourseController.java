package com.xuecheng.manage_course.controller;

import com.xuecheng.api.course.CourseControllerApi;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author Administrator
 * @version 1.0
 **/
@RestController
@RequestMapping("/course")
public class CourseController implements CourseControllerApi {

    @Autowired
    CourseService courseService;


    @Override
    @GetMapping("/teachPlan/list/{courseId}")
    public TeachplanNode findTeachplanList(@PathVariable("courseId") String courseId) {
        return courseService.findTeachplanList(courseId);
    }


    @Override
    @PostMapping("/teachPlan/add")
    public ResponseResult addTeachplan(@RequestBody  Teachplan teachplan) {
        return courseService.addTeachplan(teachplan);
    }

    @Override
    @GetMapping("/courseBase/list/{page}/{size}")
    public QueryResponseResult<CourseInfo> findCourseList(@PathVariable("page") int page,@PathVariable("size")int size, CourseListRequest courseListRequest) {
        return courseService.findCourseList(page,size,courseListRequest);
    }

    @Override
    @PostMapping("/courseBase/add")
    public AddCourseResult addCourseBase(@RequestBody CourseBase courseBase) {
        return courseService.addCourseBase(courseBase);
    }

    @Override
    @GetMapping("/courseBase/get/{courseId}")
    public CourseBase getCourseBaseById(@PathVariable("courseId") String courseId) throws
            RuntimeException {
        return courseService.getCourseBaseById(courseId);
    }

    @Override
    @PutMapping("/courseBase/update/{id}")
    public ResponseResult updateCourseBase(@PathVariable("id") String id, @RequestBody CourseBase courseBase) {
        return courseService.updateCourseBase(id, courseBase);
    }

    @Override
    @GetMapping("/courseMarket/get/{id}")
    public CourseMarket getCourseMarkById(@PathVariable("id") String id) throws RuntimeException {
        return courseService.getCourseMarkById(id);
    }

    @Override
    @PostMapping("/courseMarket/update/{id}")
    public ResponseResult updateCourseMarket(@PathVariable("id") String id, @RequestBody CourseMarket courseMarket) {
        return courseService.updataCourseMarket(id,courseMarket);
    }

    @Override
    @PostMapping("/coursePic/add")
    public ResponseResult addCoursePic(@RequestParam("courseId") String courseId, @RequestParam("pic") String pic) {
        return courseService.addCoursePic(courseId,pic);
    }

    //当用户拥有course_pic_list权限才能访问
//    @PreAuthorize("hasAuthority('course_pic_list')")
    @Override
    @GetMapping( "/coursePic/list/{courseId}")
    public CoursePic showPic(@PathVariable("courseId") String courseId) {
        return courseService.showPic(courseId);
    }

    @Override
    @DeleteMapping("/coursePic/delete")
    public ResponseResult deleteCoursePic(@RequestParam("courseId") String courseId) {
        return courseService.deleteCoursePic(courseId);
    }

    @Override
    @GetMapping("/courseView/{id}")
    public CourseView getCourseView(@PathVariable("id") String id) {
        return courseService.getCourseView(id);
    }

    @Override
    @PostMapping("/preview/{id}")
    public CoursePublishResult preview(@PathVariable("id") String id) {
        return courseService.preview(id);
    }

    @Override
    @PostMapping("/publish/{id}")
    public CoursePublishResult publish(@PathVariable("id") String id) {
        return courseService.publish(id);
    }

    @Override
    @PostMapping("/saveMedia")
    public ResponseResult saveMedia(@RequestBody TeachplanMedia teachplanMedia) {
        return courseService.saveMedia(teachplanMedia);
    }


}
