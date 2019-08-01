package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaService {

    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Value("${xc-service-manage-media.upload-location}")
    String upload_location;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    //视频处理路由
    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    public  String routingkey_media_video;

    /**
     * 根据文件md5得到文件路径
     * 规则：
     * 一级目录：md5的第一个字符
     * 二级目录：md5的第二个字符
     * 三级目录：md5
     * 文件名：md5+文件扩展名
     * @author : yechaoze
     * @date : 2019/7/18 0:01
     * @param fileMd5 文件md5值
     * @param fileExt 文件扩展名
     * @return 文件路径
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult register(String fileMd5, String fileName, long fileSize, String minetype, String fileExt) {
        //检查文件是否存在
        //检查文件目录是否存在
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        //文件路径
        String filePath = this.getFilePath(fileMd5, fileExt);
        //检验文件是否存在
        File file=new File(filePath);
        boolean exists = file.exists();

        //检查文件在数据库中的信息是否存在
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        if (exists&&optional.isPresent()){
            //文件存在
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }

        //文件不存在 检验文件目录是否存在
        File folder=new File(fileFolderPath);
        if (!folder.exists()){
            folder.mkdirs();
        }

        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 得到文件目录路径
     * @author : yechaoze
     * @date : 2019/7/18 0:24
     * @param fileMd5 : 
     * @return : java.lang.String
     */
    private String getFileFolderPath(String fileMd5){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
    }

    /**
     * 得到文件路径
     * @author : yechaoze
     * @date : 2019/7/18 0:27
     * @param fileMd5 :
     * @param fileExt :
     * @return : java.lang.String
     */
    private String getFilePath(String fileMd5,String fileExt){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/"+fileMd5+"."+fileExt;
    }

    /**
     * 获取块文件所属目录
     * @author : yechaoze
     * @date : 2019/7/18 0:40
     * @param fileMd5 :
     * @return : java.lang.String
     */
    private String getChunkPath(String fileMd5){
        return upload_location+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/chunk/";
    }

    /**
     * 分块检查
     * @author : yechaoze
     * @date : 2019/7/18 0:39
     * @param fileMd5 : 文件的md5
     * @param chunk : 块文件的下标
     * @param chunkSize : 块的大小
     * @return : com.xuecheng.framework.domain.media.response.CheckChunkResult
     */
    public CheckChunkResult checkChunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //检查分块文件是否存在
        //得到分块文件所在目录
        String chunkPath = this.getChunkPath(fileMd5);
        File chunkFile=new File(chunkPath+chunk);
        if (chunkFile.exists()){
            //块文件存在
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,true);
        }else {
            //块文件不存在
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,false);
        }

    }

    /**
     * 上传分块文件
     * @author : yechaoze
     * @date : 2019/7/18 0:47
     * @param file :
     * @param fileMd5 :
     * @param chunk :
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult uploadChunk(MultipartFile file, String fileMd5, Integer chunk) {
        //检查分块目录是否存在 不在则创建
        //得到分块目录
        String chunkPath = this.getChunkPath(fileMd5);
        //得到分块文件的路径
        String filePath=chunkPath+chunk;

        File chunkFile=new File(chunkPath);
        if (!chunkFile.exists()){
            chunkFile.mkdirs();
        }

        //得到上传文件的输入流
        InputStream fileInputStream=null;
        FileOutputStream fileOutputStream=null;
        try {
            fileInputStream = file.getInputStream();
            fileOutputStream=new FileOutputStream(new File(filePath));
            IOUtils.copy(fileInputStream,fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    /**
     * 合并分块
     * @author : yechaoze
     * @date : 2019/7/18 10:52
     * @param fileMd5 : 
     * @param fileName : 
     * @param fileSize :
     * @param mimetype :
     * @param fileExt : 
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult mergeChunks(String fileMd5, String fileName, Long fileSize, String minetype, String fileExt) {
        //得到分块文件目录
        String chunkPath = this.getChunkPath(fileMd5);
        File chunkFile=new File(chunkPath);
        //分块文件列表
        File[] files = chunkFile.listFiles();
        List<File> fileList = Arrays.asList(files);

        //创建一个合并文件
        String filePath = this.getFilePath(fileMd5, fileExt);
        //得到分块文件
        File mergeFile=new File(filePath);

        //执行分块
        mergeFile=this.getChunkFile(fileList,mergeFile);
        if (mergeFile==null){
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }

        //2、校验文件的md5值与前端传入的是否一致
        boolean checkMd5= this.checkFileMd5(mergeFile, fileMd5);
        if (!checkMd5){
            //校验失败
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }

        //3、将文件的信息传入mongodb数据库
        MediaFile mediaFile=new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        mediaFile.setFileOriginalName(fileName);
        //文件路径保存相对路径
        mediaFile.setFilePath(fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/");
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(minetype);
        mediaFile.setFileType(fileExt);
        //状态为上传成功
        mediaFile.setFileStatus("301002");
        MediaFile save = mediaFileRepository.save(mediaFile);

        //向mq发送视频处理消息
        sendProcessVideoMsg(mediaFile.getFileId());

        return new ResponseResult(CommonCode.SUCCESS);

    }

    /**
     * 得到分块文件
     * @author : yechaoze
     * @date : 2019/7/18 11:06
     * @param files :
     * @param mergeFile :
     * @return : java.io.File
     */
    private File getChunkFile(List<File> files,File mergeFile){
        try {
            if (mergeFile.exists()){
                mergeFile.delete();
            }else {
                mergeFile.createNewFile();
            }
        Collections.sort(files, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName())>Integer.parseInt(o2.getName())){
                    return 1;
                }
                return -1;
            }
        });

        //创建写对象
        RandomAccessFile raf_write=new RandomAccessFile(mergeFile,"rw");
        //创建缓冲区
        byte[] b=new byte[1024];
        for (File file:files){
            //创建读对象
            RandomAccessFile raf_read=new RandomAccessFile(file,"r");
            int len=-1;
            while ((len=raf_read.read(b))!=-1){
                raf_write.write(b,0,len);
            }
            raf_read.close();
        }
        raf_write.close();
        return mergeFile;
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 校验文件md5
     * @author : yechaoze
     * @date : 2019/7/18 11:30
     * @param mergeFile :
     * @param fileMd5 :
     * @return : boolean
     */
    private boolean checkFileMd5(File mergeFile,String fileMd5){
        try {
            //创建文件的输入流
            FileInputStream fileInputStream=new FileInputStream(mergeFile);
            //获取文件md5值
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            //和传入的md5进行比较
            if (md5Hex.equalsIgnoreCase(fileMd5)){
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }

    
    /**
     * 发送视频处理消息
     * @author : yechaoze
     * @date : 2019/7/20 16:38
     * @return : com.xuecheng.framework.model.response.ResponseResult
     */
    public ResponseResult sendProcessVideoMsg(String mediaId){
        //数据库查询mediaId
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if (!optional.isPresent()){
            ExceptionCast.cast(CommonCode.FAIL);
        }
        //构造消息内容
        Map<String,String> map=new HashMap();
        map.put("mediaId",mediaId);
        String jsonString = JSON.toJSONString(map);

        //向mq发送消息
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey_media_video,jsonString);
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }
}
