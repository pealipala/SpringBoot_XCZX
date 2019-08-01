package com.xuecheng.manage_media.service;

import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.request.QueryMediaFileRequest;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.QueryResponseResult;
import com.xuecheng.framework.model.response.QueryResult;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MediaFileService {

    @Autowired
    private MediaFileRepository mediaFileRepository;

    /**
     * 查询媒资列表
     * @author : yechaoze
     * @date : 2019/7/20 22:49
     * @param page :
     * @param size :
     * @param queryMediaFileRequest :
     * @return : com.xuecheng.framework.model.response.QueryResponseResult<com.xuecheng.framework.domain.media.MediaFile>
     */
    public QueryResponseResult<MediaFile> findList(int page, int size, QueryMediaFileRequest queryMediaFileRequest) {
        if (queryMediaFileRequest==null){
            queryMediaFileRequest=new QueryMediaFileRequest();
        }
        //创建条件对象
        MediaFile mediaFile=new MediaFile();
        if(StringUtils.isNotEmpty(queryMediaFileRequest.getTag())){
            mediaFile.setTag(queryMediaFileRequest.getTag());
        }
        if (StringUtils.isNotEmpty(queryMediaFileRequest.getFileOriginalName())){
            mediaFile.setFileOriginalName(queryMediaFileRequest.getFileOriginalName());
        }
        if (StringUtils.isNotEmpty(queryMediaFileRequest.getProcessStatus())){
            mediaFile.setProcessStatus(queryMediaFileRequest.getProcessStatus());
        }
        //定义条件匹配器
        ExampleMatcher exampleMatcher=ExampleMatcher.matching()
                .withMatcher("tag",ExampleMatcher.GenericPropertyMatchers.contains())//tag字段模糊匹配
                .withMatcher("fileOriginalName",ExampleMatcher.GenericPropertyMatchers.contains());//文件原始名称模糊匹配

        //定义example实例
        Example<MediaFile> example=Example.of(mediaFile,exampleMatcher);

        //定义分页数据
        if (page<=0){
            page=1;
        }
        page=page-1;
        if (size<=0){
            size=10;
        }
        Pageable pageable=new PageRequest(page,size);

        //查询
        Page<MediaFile> all = mediaFileRepository.findAll(example, pageable);
        long totalElements = all.getTotalElements();
        List<MediaFile> fileList = all.getContent();
        //返回数据集
        QueryResult<MediaFile> queryResult=new QueryResult<>();
        queryResult.setList(fileList);
        queryResult.setTotal(totalElements);
        QueryResponseResult queryResponseResult=new QueryResponseResult(CommonCode.SUCCESS,queryResult);
        return queryResponseResult;

    }


}
