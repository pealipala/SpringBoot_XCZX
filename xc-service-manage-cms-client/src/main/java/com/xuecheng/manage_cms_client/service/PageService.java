package com.xuecheng.manage_cms_client.service;

import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import com.xuecheng.framework.domain.cms.CmsPage;
import com.xuecheng.framework.domain.cms.CmsSite;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.manage_cms_client.dao.CmsPageRepository;
import com.xuecheng.manage_cms_client.dao.CmsSiteRepository;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.Optional;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
public class PageService {

    private static  final Logger LOGGER = LoggerFactory.getLogger(PageService.class);

    @Autowired
    GridFsTemplate gridFsTemplate;

    @Autowired
    GridFSBucket gridFSBucket;

    @Autowired
    CmsPageRepository cmsPageRepository;

    @Autowired
    CmsSiteRepository cmsSiteRepository;

    /**
     * 保存html页面到服务器物理路径
     * @author : yechaoze
     * @date : 2019/6/15 10:35
     * @param pageId :
     * @return : void
     */
    public void savePageToServerPath(String pageId){
        //根据pageId查询cmsPage
        CmsPage cmsPage = this.findCmsPageById(pageId);
        //得到html的文件id，从cmsPage中获取htmlFileId内容
        String htmlFileId = cmsPage.getHtmlFileId();

        //从gridFS中查询html文件
        InputStream inputStream = this.getFileById(htmlFileId);
        if(inputStream == null){
            LOGGER.error("getFileById InputStream is null ,htmlFileId:{}",htmlFileId);
            return ;
        }
        //得到站点id
        String siteId = cmsPage.getSiteId();
        //得到站点的信息
        CmsSite cmsSite = this.findCmsSiteById(siteId);
        //得到站点的物理路径
        String sitePhysicalPath = cmsSite.getSitePhysicalPath();
        //得到页面的物理路径
        String pagePath = sitePhysicalPath + cmsPage.getPagePhysicalPath() + cmsPage.getPageName();
        //将html文件保存到服务器物理路径上
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(pagePath));
            IOUtils.copy(inputStream,fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    /**
     * 根据文件id从GridFS中查询文件内容
     * @author : yechaoze
     * @date : 2019/6/15 10:34
     * @param fileId :
     * @return : java.io.InputStream
     */
    public InputStream getFileById(String fileId){
        //根据文件id查询文件
        GridFSFile gridFSFile = gridFsTemplate.findOne(Query.query(Criteria.where("_id").is(fileId)));
        if (gridFSFile==null){
            ExceptionCast.cast(CommonCode.INVALID_PARAM);
        }
        //打开下载流
        GridFSDownloadStream downloadStream = gridFSBucket.openDownloadStream(gridFSFile.getObjectId());
        //定义GridFsResource
        GridFsResource gridFsResource=new GridFsResource(gridFSFile,downloadStream);
        try {
            return gridFsResource.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据页面id查询页面信息
     * @author : yechaoze
     * @date : 2019/6/15 10:34
     * @param pageId :
     * @return : com.xuecheng.framework.domain.cms.CmsPage
     */
    public CmsPage findCmsPageById(String pageId){
        Optional<CmsPage> optional = cmsPageRepository.findById(pageId);
        if(optional.isPresent()){
            return optional.get();
        }
        return null;
    }

    /**
     * 根据站点id查询站点信息
     * @author : yechaoze
     * @date : 2019/6/15 10:34
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
