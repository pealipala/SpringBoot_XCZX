package com.xuecheng.manage_course.service;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.domain.course.*;
import com.xuecheng.framework.domain.course.ext.CourseInfo;
import com.xuecheng.framework.domain.course.ext.CourseView;
import com.xuecheng.framework.domain.course.ext.TeachplanNode;
import com.xuecheng.framework.domain.course.request.CourseListRequest;
import com.xuecheng.framework.domain.course.response.AddCourseResult;
import com.xuecheng.framework.domain.course.response.CourseCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_course.client.CmsPageClient;
import com.xuecheng.manage_course.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sun.java2d.cmm.CMSManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
public class CourseService {
    @Autowired
    private TeachplanMapper teachplanMapper;
    @Autowired
    private TeachplanRepository teachplanRepository;
    @Autowired
    private CourseBaseRepository courseBaseRepository;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseMarketRepository courseMarketRepository;
    @Autowired
    private CoursePicRepository coursePicRepository;
    @Autowired
    private CoursePubRepository coursePubRepository;
    @Autowired
    private TeachplanMediaRepository teachplanMediaRepository;
    @Autowired
    private CmsPageClient cmsPageClient;
    @Autowired
    private TeachplanMediaPubRepository teachplanMediaPubRepository;

    @Value("${course‐publish.dataUrlPre}")
    private String publish_dataUrlPre;
    @Value("${course‐publish.pagePhysicalPath}")
    private String publish_page_physicalpath;
    @Value("${course‐publish.pageWebPath}")
    private String publish_page_webpath;
    @Value("${course‐publish.siteId}")
    private String publish_siteId;
    @Value("${course‐publish.templateId}")
    private String publish_templateId;
    @Value("${course‐publish.previewUrl}")
    private String previewUrl;

    //查询课程计划
    public TeachplanNode findTeachplanList(String courseId){
        return teachplanMapper.selectList(courseId);
    }

    /**
     * 添加课程计划
     * @author : yechaoze
     * @date : 2019/6/17 12:47
     * @param teachplan :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    @Transactional
    public ResponseResult addTeachplan(Teachplan teachplan) {
        //检验课程id和课程名称
        if (teachplan==null ||
                StringUtils.isEmpty(teachplan.getCourseid()) ||
                StringUtils.isEmpty(teachplan.getPname())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //取出课程id
        String courseid = teachplan.getCourseid();
        //取出父结点id
        String parentid = teachplan.getParentid();
        if (StringUtils.isEmpty(parentid)){
            parentid=this.getTeachplanRoot(courseid);
        }
        //取出父节点信息
        Optional<Teachplan> optional = teachplanRepository.findById(parentid);
        Teachplan teachplanParent= optional.get();
        String grade=teachplanParent.getGrade();
        //设置新节点
        Teachplan teachplanNew=new Teachplan();
        BeanUtils.copyProperties(teachplan,teachplanNew);
        teachplanNew.setParentid(parentid);
        teachplanNew.setCourseid(courseid);
        if (grade.equals("1")){
            teachplanNew.setGrade("2");
        }else {
            teachplanNew.setGrade("3");
        }
        teachplanRepository.save(teachplanNew);

        //返回成功
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 获取课程的根节点
     * @author : yechaoze
     * @date : 2019/6/17 12:47
     * @param courseId :
     * @return : java.lang.String
     */
    public String getTeachplanRoot(String courseId){
        //查询当前课程
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()){
            return null;
        }
        CourseBase courseBase=optional.get();
        //查询根节点
        List<Teachplan> teachplans = teachplanRepository.findByCourseidAndParentid(courseId, "0");
        //无数据 -- 自定义当前课程为根节点
        if (teachplans==null||teachplans.size()<=0){

            Teachplan teachplanRoot=new Teachplan();
            teachplanRoot.setGrade("1");//当前为一级节点
            teachplanRoot.setCourseid(courseId);
            teachplanRoot.setStatus("0");//0为未发布
            teachplanRoot.setParentid("0");
            teachplanRoot.setPname(courseBase.getName());
            teachplanRepository.save(teachplanRoot);

            return teachplanRoot.getId();
        }
        //有数据
        return teachplans.get(0).getId();
    }

    /**
     * 查询课程并且分页
     * @author : yechaoze
     * @date : 2019/6/20 20:25
     * @param page :
     * @param size :
     * @param courseListRequest :
     * @return : com.xuecheng.framework.model.response.QueryResponseResult<com.xuecheng.framework.domain.course.ext.CourseInfo>
     */
    public QueryResponseResult<CourseInfo> findCourseList(int page, int size, CourseListRequest courseListRequest){

        if (courseListRequest==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        if (page<=0){
            page=1;
        }
        if (size<=0){
            size=20;
        }
        //设置分页参数
        PageHelper.startPage(page,size);
        //分页查询
        Page<CourseInfo> courseListPage = courseMapper.findCourseListPage(courseListRequest);
        //查询列表
        List<CourseInfo> result = courseListPage.getResult();
        //查询总数
        long  total=courseListPage.getTotal();
        //查询结果集
        QueryResult<CourseInfo> queryResult = new QueryResult<CourseInfo>();
        queryResult.setList(result);
        queryResult.setTotal(total);
        return new QueryResponseResult<CourseInfo>(CommonCode.SUCCESS,queryResult);

    }

    /**
     * 新增课程
     * @author : yechaoze
     * @date : 2019/6/21 15:51
     * @param courseBase :
     * @return : com.xuecheng.framework.domain.course.response.AddCourseResult
     */
    @Transactional
    public AddCourseResult addCourseBase(CourseBase courseBase){
        courseBase.setStatus("202001");
        courseBaseRepository.save(courseBase);
        return new AddCourseResult(CommonCode.SUCCESS,courseBase.getId());
    }

    /**
     * 管理课程内 的基本信息获取
     * @author : yechaoze
     * @date : 2019/6/21 13:55
     * @param courseId :
     * @return : com.xuecheng.framework.domain.course.CourseBase
     */
    public CourseBase getCourseBaseById(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    /**
     * 修改课程基本信息
     * @author : yechaoze
     * @date : 2019/6/21 16:08
     * @param id :
     * @param courseBase :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    @Transactional
    public ResponseResult updateCourseBase(String id, CourseBase courseBase){
        CourseBase one = this.getCourseBaseById(id);
        if (one==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //修改课程信息
        one.setName(courseBase.getName());
        one.setMt(courseBase.getMt());
        one.setSt(courseBase.getSt());
        one.setGrade(courseBase.getGrade());
        one.setStudymodel(courseBase.getStudymodel());
        one.setUsers(courseBase.getUsers());
        one.setDescription(courseBase.getDescription());
        courseBaseRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 管理课程内的 课程营销获取
     * @author : yechaoze
     * @date : 2019/6/21 20:13
     * @param id:
     * @return : com.xuecheng.framework.domain.course.CourseMarket
     */
    public CourseMarket getCourseMarkById(String id){
        Optional<CourseMarket> optional = courseMarketRepository.findById(id);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    /**
     * 修改课程营销信息
     * @author : yechaoze
     * @date : 2019/6/21 20:29
     * @param id :
     * @param courseMarket :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    @Transactional
    public ResponseResult updataCourseMarket(String id,CourseMarket courseMarket){
        CourseMarket one = this.getCourseMarkById(id);
        if (one==null){
            one=new CourseMarket();
            BeanUtils.copyProperties(courseMarket,one);
            one.setId(id);
            courseMarketRepository.save(one);
        }else{
            one.setCharge(courseMarket.getCharge());
            one.setStartTime(courseMarket.getStartTime());//课程有效期，开始时间
            one.setEndTime(courseMarket.getEndTime());//课程有效期，结束时间
            one.setPrice(courseMarket.getPrice());
            one.setQq(courseMarket.getQq());
            one.setValid(courseMarket.getValid());
            courseMarketRepository.save(one);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 课程的图片保存服务
     * @author : yechaoze
     * @date : 2019/6/23 12:24
     * @param courseId :
     * @param pic :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    @Transactional
    public ResponseResult addCoursePic(String courseId,String pic){
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        CoursePic coursePic=null;
        if (optional.isPresent()){
            coursePic=optional.get();
        }
        if (coursePic==null){
            coursePic=new CoursePic();
        }
        coursePic.setCourseid(courseId);
        coursePic.setPic(pic);
        coursePicRepository.save(coursePic);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 查看课程图片
     * @author : yechaoze
     * @date : 2019/6/23 19:41
     * @param courseId :
     * @return : com.xuecheng.framework.domain.course.CoursePic
     */
    public CoursePic showPic(String courseId){
        Optional<CoursePic> optional = coursePicRepository.findById(courseId);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    /**
     * 删除图片
     * @author : yechaoze
     * @date : 2019/6/23 19:54
     * @param courseId :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    @Transactional
    public ResponseResult deleteCoursePic(String courseId){
        Long delete = coursePicRepository.deleteBycourseid(courseId);
        if (delete>0){
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);
    }

    /**
     * 获取CourseView
     * @author : yechaoze
     * @date : 2019/6/30 18:08
     * @param id : 
     * @return : com.xuecheng.framework.domain.course.ext.CourseView
     */
    public CourseView getCourseView(String id) {
        CourseView courseView=new CourseView();
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()){
            courseView.setCoursePic(picOptional.get());
        }
        Optional<CourseMarket> marketOptional = courseMarketRepository.findById(id);
        if (marketOptional.isPresent()){
            courseView.setCourseMarket(marketOptional.get());
        }
        Optional<CourseBase> baseOptional = courseBaseRepository.findById(id);
        if (baseOptional.isPresent()){
            courseView.setCourseBase(baseOptional.get());
        }
        TeachplanNode list = teachplanMapper.selectList(id);
        courseView.setTeachplanNode(list);
        return courseView;
    }

    /**
     * 查找课程
     * @author : yechaoze
     * @date : 2019/7/3 20:51
     * @return : com.xuecheng.framework.domain.course.CourseBase
     */
    public CourseBase findCourseBaseById(String courseId){
        Optional<CourseBase> optional = courseBaseRepository.findById(courseId);
        if (!optional.isPresent()){
            ExceptionCast.cast(CourseCode.COURSE_GET_NULL);
            return null;
        }
        return optional.get();
    }

    /**
     * 课程预览
     * @author : yechaoze
     * @date : 2019/7/3 20:19
     * @param id : 
     * @return : com.xuecheng.framework.domain.course.CoursePublishResult
     */
    public CoursePublishResult preview(String id) {
        CourseBase course = this.findCourseBaseById(id);
        //添加页面
        //准备cmsPage信息
        CmsPage cmsPage=new CmsPage();
        cmsPage.setSiteId(publish_siteId);//站点id
        cmsPage.setTemplateId(publish_templateId);//模板id
        cmsPage.setDataUrl(publish_dataUrlPre+id);//dataUrl
        cmsPage.setPageName(course.getId());//名称
        cmsPage.setPageAliase(course.getName());//页面别名
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//物理路径
        cmsPage.setPageWebPath(publish_page_webpath);//页面webPath
        //远程调用
        CmsPageResult cmsPageResult = cmsPageClient.saveCmsPage(cmsPage);
        if (!cmsPageResult.isSuccess()){
            //抛出异常
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        //获取页面id
        String pageId = cmsPageResult.getCmsPage().getPageId();
        //生成页面url
        String url=previewUrl+pageId;
        //返回
        return new CoursePublishResult(CommonCode.SUCCESS, url);
    }

    /**
     * 远程调用一键发布实现 发布页面
     * @author : yechaoze
     * @date : 2019/7/4 22:31
     * @param id :
     * @return : com.xuecheng.framework.domain.course.CoursePublishResult
     */
    @Transactional
    public CoursePublishResult publish(String id) {
        //根据id获取课程信息
        CmsPage cmsPage=new CmsPage();
        CourseBase course = this.findCourseBaseById(id);
        cmsPage.setSiteId(publish_siteId);//站点id
        cmsPage.setTemplateId(publish_templateId);//模板id
        cmsPage.setDataUrl(publish_dataUrlPre+id);//dataUrl
        cmsPage.setPageName(course.getId());//名称
        cmsPage.setPageAliase(course.getName());//页面别名
        cmsPage.setPagePhysicalPath(publish_page_physicalpath);//物理路径
        cmsPage.setPageWebPath(publish_page_webpath);//页面webPath
        //远程调用一键发布
        CmsPostPageResult cmsPostPageResult = cmsPageClient.postPageQuick(cmsPage);
        if (!cmsPostPageResult.isSuccess()){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }
        //设置发布状态为"已发布"
        CourseBase courseBase = this.courseBaseStatus(id);
        if (courseBase==null){
            return new CoursePublishResult(CommonCode.FAIL,null);
        }

        //创建CoursePub对象
        CoursePub coursePub = this.createCoursePub(id);
        //保存课程索引信息
        this.saveCoursePub(id,coursePub);
        //缓存课程信息
        //...

        //得到页面的Url
        String pageUrl = cmsPostPageResult.getPageUrl();

        //保存课程计划媒资信息\
        this.saveTeachplanMediaPub(id);

        return new CoursePublishResult(CommonCode.SUCCESS,pageUrl);
    }

    /**
     * 保存课程媒资信息
     * @author : yechaoze
     * @date : 2019/7/23 0:58
     * @param courseId :
     * @return : void
     */
    private void saveTeachplanMediaPub(String courseId){
        //删除TeachplanMediaPub中的信息
        teachplanMediaPubRepository.deleteByCourseId(courseId);
        //查询TeachplanMedia中的信息
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByCourseId(courseId);
        //将查询到的数据添加到TeachplanMediaPub中
        List<TeachplanMediaPub> teachplanMediaPubs=new ArrayList<>();
        for (TeachplanMedia list:teachplanMediaList){
            TeachplanMediaPub teachplanMediaPub=new TeachplanMediaPub();
            BeanUtils.copyProperties(list,teachplanMediaPub);
            //添加时间戳
            teachplanMediaPub.setTimestamp(new Date());
            teachplanMediaPubs.add(teachplanMediaPub);
        }
        teachplanMediaPubRepository.saveAll(teachplanMediaPubs);
    }

    /**
     * 创建CoursePub对象
     * @author : yechaoze
     * @date : 2019/7/9 12:28
     * @param id :
     * @return : com.xuecheng.framework.domain.course.CoursePub
     */
    private CoursePub createCoursePub(String id){
        CoursePub coursePub=new CoursePub();
        coursePub.setId(id);
        //根据id获取CourseBase
        CourseBase courseBase = this.getCourseBaseById(id);
        if (courseBase!=null){
            BeanUtils.copyProperties(courseBase,coursePub);
        }
        //根据id获取CoursePic
        Optional<CoursePic> picOptional = coursePicRepository.findById(id);
        if (picOptional.isPresent()){
            CoursePic coursePic = picOptional.get();
            BeanUtils.copyProperties(coursePic,coursePub);
        }
        //根据id获取CourseMarket
        CourseMarket courseMarket = this.getCourseMarkById(id);
        if (courseMarket!=null){
            BeanUtils.copyProperties(courseMarket,coursePub);
        }
        //获取课程计划信息
        TeachplanNode teachplanNode = teachplanMapper.selectList(id);
        String jsonString = JSON.toJSONString(teachplanNode);
        coursePub.setTeachplan(jsonString);
        return coursePub;
    }

    /**
     * 保存课程索引信息
     * @author : yechaoze
     * @date : 2019/7/23 0:56
     * @param id :
     * @param coursePub :
     * @return : com.xuecheng.framework.domain.course.CoursePub
     */
    public CoursePub saveCoursePub(String id,CoursePub coursePub){
        CoursePub coursePubNew=null;
        Optional<CoursePub> optional = coursePubRepository.findById(id);
        if (optional.isPresent()){
                coursePubNew=optional.get();
        }
        coursePubNew=new CoursePub();
        BeanUtils.copyProperties(coursePub,coursePubNew);
        //因为id被覆盖 重新赋值
        coursePubNew.setId(id);
        //设置时间戳 logStach使用
        coursePubNew.setTimestamp(new Date());
        //设置时间格式
        SimpleDateFormat simpleDateFormat=new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        String date = simpleDateFormat.format(new Date());
        //设置时间
        coursePubNew.setPubTime(date);
        coursePubRepository.save(coursePubNew);
        return coursePubNew;
    }

    /**
     * 更改课程状态为已发布:202002
     * @author : yechaoze
     * @date : 2019/7/4 23:13
     * @param id :
     * @return : com.xuecheng.framework.domain.course.CourseBase
     */
    private CourseBase courseBaseStatus(String id){
        CourseBase courseBase = this.findCourseBaseById(id);
        courseBase.setStatus("202002");
        courseBaseRepository.save(courseBase);
        return courseBase;
    }

    /**
     * 课程与媒资文件的关联
     * @author : yechaoze
     * @date : 2019/7/21 0:45
     * @param teachplanMedia :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult saveMedia(TeachplanMedia teachplanMedia) {
        if (teachplanMedia==null||StringUtils.isEmpty(teachplanMedia.getTeachplanId())){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }

        //1、根据课程id 校验
        String teachplanId = teachplanMedia.getTeachplanId();
        Optional<Teachplan> optional = teachplanRepository.findById(teachplanId);
        if (!optional.isPresent()){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }

        //2、校验 是否为3级目录
        Teachplan teachplan = optional.get();
        if (teachplan.getGrade()==null||!teachplan.getGrade().equals("3")){
            ExceptionCast.cast(CourseCode.COURSE_MEDIA_TEACHPLAN_GRADEERROR);
        }

        //3、存在则赋值 不存在则重新创建对象
        TeachplanMedia one=null;
        Optional<TeachplanMedia> media = teachplanMediaRepository.findById(teachplanId);
        if (media.isPresent()){
            one=media.get();
        }else {
            one = new TeachplanMedia();
        }
        //4、保存数据
        one.setCourseId(teachplan.getCourseid());
        one.setMediaFileOriginalName(teachplanMedia.getMediaFileOriginalName());
        one.setMediaId(teachplanMedia.getMediaId());
        one.setMediaUrl(teachplanMedia.getMediaUrl());
        one.setTeachplanId(teachplanId);
        teachplanMediaRepository.save(one);
        return new ResponseResult(CommonCode.SUCCESS);

    }
}
