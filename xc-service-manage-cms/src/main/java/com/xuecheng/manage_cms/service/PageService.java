package com.xuecheng.manage_cms.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.sun.org.apache.bcel.internal.generic.IF_ACMPEQ;
import com.sun.org.apache.regexp.internal.RE;
import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.domain.cms.CmsTemplate;
import com.xuecheng.framework.domain.cms.request.QueryPageRequest;
import com.xuecheng.framework.domain.cms.response.CmsCode;
import com.xuecheng.framework.domain.cms.response.CmsPageResult;
import com.xuecheng.framework.domain.cms.response.CmsPostPageResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_cms.config.RabbitmqConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import com.xuecheng.manage_cms.dao.CmsPageRepository;
import com.xuecheng.manage_cms.dao.CmsSiteRepository;
import com.xuecheng.manage_cms.dao.CmsTemplateResitory;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 * @create 2018-09-12 18:32
 **/
@Service
public class PageService {

    @Autowired
    private CmsPageRepository cmsPageRepository;
    @Autowired
    private CmsConfigRepository cmsConfigRepository;
    @Autowired
    CmsTemplateResitory cmsTemplateResitory;
    @Autowired
    private CmsSiteRepository cmsSiteRepository;
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private GridFsTemplate gridFsTemplate;
    @Autowired
    private GridFSBucket gridFSBucket;
    @Autowired
    private RabbitTemplate rabbitTemplate;



    /**
     * 页面查询方法
     * @param page 页码，从1开始记数
     * @param size 每页记录数
     * @param queryPageRequest 查询条件
     * @return
     */
    public QueryResponseResult findList(int page, int size, QueryPageRequest queryPageRequest){

        if (queryPageRequest==null){
            queryPageRequest=new QueryPageRequest();
        }
        //自定义条件查询
        //设置条件匹配器
        ExampleMatcher exampleMatcher=ExampleMatcher.matching()
                .withMatcher("pageAliase",ExampleMatcher.GenericPropertyMatchers.contains());
        //条件值对象
        CmsPage cmsPage=new CmsPage();
        //设置条件值 站点id
        if (StringUtils.isNotEmpty(queryPageRequest.getSiteId())){
            cmsPage.setSiteId(queryPageRequest.getSiteId());
        }
        //设置条件值 模板id
        if (StringUtils.isNotEmpty(queryPageRequest.getTemplateId())){
            cmsPage.setTemplateId(queryPageRequest.getTemplateId());
        }
        //设置条件值 别名
        if (StringUtils.isNotEmpty(queryPageRequest.getPageAliase())){
            cmsPage.setPageAliase(queryPageRequest.getPageAliase());
        }
        //定义条件对象 Example
        Example<CmsPage> example=Example.of(cmsPage,exampleMatcher);

        //分页参数
        if(page <=0){
            page = 1;
        }
        page = page -1;
        if(size<=0){
            size = 10;
        }
        Pageable pageable = PageRequest.of(page,size);
        //定义条件查询 并且分页
        Page<CmsPage> all = cmsPageRepository.findAll(example,pageable);

        QueryResult queryResult = new QueryResult();
        queryResult.setList(all.getContent());//数据列表
        queryResult.setTotal(all.getTotalElements());//数据总记录数
        QueryResponseResult queryResponseResult = new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;

    }

    /**
     * 新增页面
     * @author : yechaoze
     * @date : 2019/6/5 12:52
     * @param cmsPage :
     * @return : com.xuecheng.framework.domain.cms.response.CmsPageResult
     */
    public CmsPageResult addPage(CmsPage cmsPage){

        if (cmsPage==null){
            //抛出异常 非法请求
            ExceptionCast.cast(CmsCode.CMS_COURSE_PERVIEWISNULL);
        }
        //校验数据的唯一性
        //调用校验的 页面名称 站点id 页面webPath
        CmsPage cmsPage1 = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (cmsPage1!=null){
            //页面存在 抛出异常
            ExceptionCast.cast(CmsCode.CMS_ADDPAGE_EXISTSNAME);
        }

            // 设置pageID为空 保证pageID由mongodb创建
            cmsPage.setPageId(null);
            //调用dao 插入页面
            CmsPage save = cmsPageRepository.save(cmsPage);
            return new CmsPageResult(CommonCode.SUCCESS,save);

    }

    /**
     * 根据id查询页面
     * @author : yechaoze
     * @date : 2019/6/5 16:09
     * @param id :
     * @return : com.xuecheng.framework.domain.cms.CmsPage
     */
    public CmsPage findByID(String id){

        Optional<CmsPage> page = cmsPageRepository.findById(id);
        if (!page.isPresent()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        return page.get();


    }

    /**
     * 更新页面
     * @author : yechaoze
     * @date : 2019/6/5 16:30
     * @param id :
     * @param cmsPage :
     * @return : com.xuecheng.framework.domain.cms.response.CmsPageResult
     */
    public CmsPageResult editPage(String id,CmsPage cmsPage){

        //根据id查询页面
        CmsPage page = this.findByID(id);
        if (page!=null){
            //更新模板id
            page.setTemplateId(cmsPage.getTemplateId());
            //更新所属站点
            page.setSiteId(cmsPage.getSiteId());
            //更新页面别名
            page.setPageAliase(cmsPage.getPageAliase());
            //更新页面名称
            page.setPageName(cmsPage.getPageName());
            //更新访问路径
            page.setPageWebPath(cmsPage.getPageWebPath());
            //更新物理路径
            page.setPagePhysicalPath(cmsPage.getPagePhysicalPath());
            //更新DataUrl
            page.setDataUrl(cmsPage.getDataUrl());
            //执行更新
            CmsPage save = cmsPageRepository.save(page);
            if (save!=null){
                return new CmsPageResult(CommonCode.SUCCESS,page);
            }
        }
        return new CmsPageResult(CommonCode.FAIL,null);
    }

    /**
     * 删除页面
     * @author : yechaoze
     * @date : 2019/6/7 18:47
     * @param id :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult delPage(String id){

        //根据id查询页面
        CmsPage page = this.findByID(id);
        if (page!=null){
            cmsPageRepository.deleteById(id);
            return new ResponseResult(CommonCode.SUCCESS);
        }
        return new ResponseResult(CommonCode.FAIL);

    }

    /**
     * 根据id查询配置管理信息
     * @author : yechaoze
     * @date : 2019/6/7 18:49
     * @param id :
     * @return : com.xuecheng.framework.domain.cms.CmsConfig
     */
    public CmsConfig getModel(String id){
        Optional<CmsConfig> optional = cmsConfigRepository.findById(id);
        if (optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    /**
     * 获取静态页面数据
     * @author : yechaoze
     * @date : 2019/6/8 19:05
     * @param pageId :
     * @return : java.lang.String
     */
    public String getHtml(String pageId){

        //获取数据模型
        Map model = this.getModelByPageId(pageId);
        if (model==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAISNULL);
        }

        //获取页面模板
        String template = this.getTemplate(pageId);
        if (StringUtils.isEmpty(template)){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }

        //执行静态化
        String generateHtml = this.generateHtml(template, model);
        return generateHtml;

    }

    /**
     * 获取数据模型（根据获取的DataUrl请求数据）
     * @author : yechaoze
     * @date : 2019/6/8 19:04
     * @param pageId :
     * @return : java.util.Map
     */
    private Map getModelByPageId(String pageId){

        CmsPage cmsPage = this.findByID(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_NOTEXISTS);
        }
        String dataUrl = cmsPage.getDataUrl();
        if (dataUrl==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_DATAURLISNULL);
        }
        //根据restTemplate请求DataUri获取数据
        ResponseEntity<Map> forEntity = restTemplate.getForEntity(dataUrl, Map.class);
        Map body = forEntity.getBody();
        return body;

    }

    /**
     * 获取页面模板信息
     * @author : yechaoze
     * @date : 2019/6/8 19:10
     * @param pageId :
     * @return : java.lang.String
     */
    public String getTemplate(String pageId){

        //获取页面信息
        CmsPage cmsPage = this.findByID(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_NOTEXISTS);
        }
        String templateId = cmsPage.getTemplateId();
        if (templateId==null){
            ExceptionCast.cast(CmsCode.CMS_GENERATEHTML_TEMPLATEISNULL);
        }
        //查询模板信息
        Optional<CmsTemplate> template = cmsTemplateResitory.findById(templateId);
        if (template.isPresent()){
            CmsTemplate cmsTemplate = template.get();
            //获取模板文件id
            String templateFileId = cmsTemplate.getTemplateFileId();
            GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(templateFileId)));
            //打开下载流对象
            GridFSDownloadStream gridFSDownloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
            //创建GridFsResource
            GridFsResource gridFsResource = new GridFsResource(gridFSFile,gridFSDownloadStream);
            try {
                String content = IOUtils.toString(gridFsResource.getInputStream(), "UTF-8");
                return content;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;

    }

    /**
     * 执行静态化
     * @author : yechaoze
     * @date : 2019/6/8 20:41
     * @param template :
     * @param model :
     * @return : java.lang.String
     */
    private String generateHtml(String template,Map model){

        //创建配置对象
        Configuration configuration=new Configuration(Configuration.getVersion());
        //创建模板加载器
        StringTemplateLoader stringTemplateLoader=new StringTemplateLoader();
        stringTemplateLoader.putTemplate("template",template);
        //向配置对象配置模板加载器
        configuration.setTemplateLoader(stringTemplateLoader);
        //获取模板
        try {
            Template configurationTemplate = configuration.getTemplate("template");
            //调用api进行静态化
            String content = FreeMarkerTemplateUtils.processTemplateIntoString(configurationTemplate , model);
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 页面发布
     * @author : yechaoze
     * @date : 2019/6/14 19:22
     * @param pageId :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult post(String pageId){
        //执行页面静态化
        String html = this.getHtml(pageId);
        //静态化文件传到GridFs
        CmsPage cmsPage = saveHtml(pageId, html);
        //向MQ发送消息
        sendToMq(pageId);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 保存html到GridFs
     * @author : yechaoze
     * @date : 2019/6/14 19:21
     * @param pageId :
     * @param htmlContent :
     * @return : com.xuecheng.framework.domain.cms.CmsPage
     */
    private CmsPage saveHtml(String pageId,String htmlContent){
        //查询页面信息
        CmsPage cmsPage = this.findByID(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        ObjectId objectId = null;
        try {
            //将htmlContent转成输入流
            InputStream inputStream = IOUtils.toInputStream(htmlContent, "utf-8");
            //将文件保存到GridFs
            objectId = gridFsTemplate.store(inputStream, cmsPage.getPageName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //更新文件id
        cmsPage.setHtmlFileId(objectId.toHexString());
        cmsPageRepository.save(cmsPage);
        return cmsPage;
    }

    /**
     * 向mq发送消息
     * @author : yechaoze
     * @date : 2019/6/14 19:25
     * @param pageId :
     * @return : void
     */
    private void sendToMq(String pageId){
        CmsPage cmsPage = this.findByID(pageId);
        if (cmsPage==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        Map<String,String> json=new HashMap<>();
        json.put("pageId",pageId);
        //消息内容
        String jsonString = JSON.toJSONString(json);
        //发布消息
        rabbitTemplate.convertAndSend(RabbitmqConfig.EX_ROUTING_CMS_POSTPAGE,cmsPage.getSiteId(),jsonString);
    }

    /**
     * 页面的保存 有则更新 没有则添加
     * @author : yechaoze
     * @date : 2019/7/3 19:05
     * @param cmsPage :
     * @return : com.xuecheng.framework.domain.cms.response.CmsPageResult
     */
    public CmsPageResult savePage(CmsPage cmsPage) {
        //检验数据的唯一性
        CmsPage page = cmsPageRepository.findByPageNameAndSiteIdAndPageWebPath(cmsPage.getPageName(), cmsPage.getSiteId(), cmsPage.getPageWebPath());
        if (page==null){
            //为空 添加页面
            return this.addPage(cmsPage);
        }
        //不为空 更新页面
        return this.editPage(page.getPageId(),cmsPage);
    }

    /**
     * 课程一键发布
     * @author : yechaoze
     * @date : 2019/7/4 22:03
     * @param cmsPage :
     * @return : com.xuecheng.framework.domain.cms.response.CmsPostPageResult
     */
    public CmsPostPageResult postPageQuick(CmsPage cmsPage) {
        //保存页面信息
        CmsPageResult cmsPageResult = this.savePage(cmsPage);
        if (!cmsPageResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        CmsPage pageResultCmsPage = cmsPageResult.getCmsPage();
        //执行页面的发布
        ResponseResult responseResult = this.post(pageResultCmsPage.getPageId());
        if (!responseResult.isSuccess()){
            return new CmsPostPageResult(CommonCode.FAIL,null);
        }
        //得到页面的url
        //页面url=站点域名+站点webpath+页面webpath+页面名称
        String siteId = pageResultCmsPage.getSiteId();
        CmsSite cmsSite = this.findCmsSiteById(siteId);
        String pageUrl=cmsSite.getSiteDomain()+cmsSite.getSiteWebPath()+pageResultCmsPage.getPageWebPath()+pageResultCmsPage.getPageName();
        return new CmsPostPageResult(CommonCode.SUCCESS,pageUrl);
    }

    /**
     * 根据站点id查询站点信息
     * @author : yechaoze
     * @date : 2019/7/4 22:22
     * @param siteId :
     * @return : com.xuecheng.framework.domain.cms.CmsSite
     */
    public CmsSite findCmsSiteById(String siteId){
        Optional<CmsSite> optional = cmsSiteRepository.findById(siteId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }


}
