package com.xuecheng.filesystem.service;

import com.alibaba.fastjson.JSON;
import com.mongodb.TaggableReadPreference;
import com.mongodb.gridfs.CLI;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.omg.CORBA.PUBLIC_MEMBER;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import springfox.documentation.spring.web.json.Json;

import javax.swing.plaf.PanelUI;
import java.io.IOException;
import java.util.Map;

/**
 * @author Administrator
 * @version 1.0
 **/
@Service
public class FileSystemService {

    @Value("${xuecheng.fastdfs.tracker_servers}")
    String tracker_servers;
    @Value("${xuecheng.fastdfs.connect_timeout_in_seconds}")
    int connect_timeout_in_seconds;
    @Value("${xuecheng.fastdfs.network_timeout_in_seconds}")
    int network_timeout_in_seconds;
    @Value("${xuecheng.fastdfs.charset}")
    String charset;

    @Autowired
    private FileSystemRepository fileSystemRepository;

    /**
     * 初始化配置
     * @author : yechaoze
     * @date : 2019/6/22 23:31
     * @return : void
     */
    private void Init(){
        try {
            ClientGlobal.initByTrackers(tracker_servers);
            ClientGlobal.setG_charset(charset);
            ClientGlobal.setG_connect_timeout(connect_timeout_in_seconds);
            ClientGlobal.setG_network_timeout(network_timeout_in_seconds);
        } catch (Exception e) {
            ExceptionCast.cast(FileSystemCode.FS_INITFDFSERROR);
            e.printStackTrace();
        }
    }

    /**
     * 上传文件fastdfs
     * @author : yechaoze
     * @date : 2019/6/22 23:33
     * @param multipartFile :
     * @return : java.lang.String
     */
    private String upload(MultipartFile multipartFile){
        try {
            //初始化配置
            this.Init();
            //创建TrackerClient
            TrackerClient trackerClient=new TrackerClient();
            //获取连接
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取Storage
            StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);
            //创建StorageClient
            StorageClient1 storageClient1=new StorageClient1(trackerServer,storageServer);
            //获取文件的字节数组
            byte[] bytes = multipartFile.getBytes();
            //获取文件名
            String originalFilename = multipartFile.getOriginalFilename();
            //获取文件拓展名
            String substring = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            //上传文件
            String fileId = storageClient1.upload_file1(bytes, substring, null);
            return fileId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 实现图片上传
     * @author : yechaoze
     * @date : 2019/6/22 23:38
     * @param multipartFile :
     * @param filetag :
     * @param businesskey :
     * @param metadata :
     * @return : com.xuecheng.framework.domain.filesystem.response.UploadFileResult
     */
    public UploadFileResult uploadFile(MultipartFile multipartFile,String filetag,
                                       String businesskey,
                                       String metadata){
        if (multipartFile==null){
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
        }
        String originalFilename = multipartFile.getOriginalFilename();
        //上传文件到fastdfs
        String fileId = this.upload(multipartFile);
        FileSystem fileSystem=new FileSystem();
        //ID
        fileSystem.setFileId(fileId);
        fileSystem.setFileName(originalFilename);
        fileSystem.setFilePath(fileId);
        fileSystem.setFiletag(filetag);
        fileSystem.setBusinesskey(businesskey);
        fileSystem.setFileSize(multipartFile.getSize());
        fileSystem.setFileType(multipartFile.getContentType());
        if (StringUtils.isNotEmpty(metadata)){

            try {
                Map map = JSON.parseObject(metadata, Map.class);
                fileSystem.setMetadata(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        fileSystemRepository.save(fileSystem);
        return new UploadFileResult(CommonCode.SUCCESS,fileSystem);

    }





}
