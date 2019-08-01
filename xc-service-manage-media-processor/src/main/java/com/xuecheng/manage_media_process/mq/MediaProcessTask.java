package com.xuecheng.manage_media_process.mq;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.MediaFileProcess_m3u8;
import com.xuecheng.framework.utils.HlsVideoUtil;
import com.xuecheng.framework.utils.Mp4VideoUtil;
import com.xuecheng.manage_media_process.dao.MediaFileRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class MediaProcessTask {

    @Autowired
    private MediaFileRepository mediaFileRepository;
    @Value("${xc-service-manage-media.ffmpeg-path}")
    String ffmpeg_path;
    @Value("${xc-service-manage-media.video-location}")
    String video_location;

    @RabbitListener(queues = "${xc-service-manage-media.mq.queue-media-video-processor}",containerFactory = "customContainerFactory")
    public void receiveMediaProcessTask(String msg){
        //1、解析消息内容 得到mediaId
        Map map = JSON.parseObject(msg, Map.class);
        String mediaId = (String) map.get("mediaId");

        //2、拿mediaId从数据库查询信息
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if (!optional.isPresent()){
            return;
        }
        MediaFile mediaFile=optional.get();

        //3、使用工具类将avi生成mp4
        if (!mediaFile.getFileType().equals("avi")){
            //无需处理
            mediaFile.setProcessStatus("303004");
            mediaFileRepository.save(mediaFile);
            return;
        }else{
            //处理中
            mediaFile.setProcessStatus("303001");
            mediaFileRepository.save(mediaFile);
        }
        //String ffmpeg_path, String video_path, String mp4_name, String mp4folder_path
        String video_path=video_location+mediaFile.getFilePath()+mediaFile.getFileName();//需要转换的avi文件路径
        String mp4_name=mediaFile.getFileId()+".mp4";//生成的文件名字
        String mp4folder_path=video_location+mediaFile.getFilePath();//生成文件存放的目录
        //创建工具类对象
        Mp4VideoUtil mp4VideoUtil=new Mp4VideoUtil(ffmpeg_path,video_path,mp4_name,mp4folder_path);
        String result = mp4VideoUtil.generateMp4();
        if (result==null||!result.equals("success")){
            //处理失败
            mediaFile.setProcessStatus("303003");
            //定义MediaFileProcess_m3u8
            MediaFileProcess_m3u8 mediaFileProcess_m3u8=new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);//记录失败原因
            mediaFileRepository.save(mediaFile);
            return;
        }

        //4、将mp4生成m3u8和ts文件
        //String ffmpeg_path, String video_path, String m3u8_name,String m3u8folder_path
        String mp4_video_path=video_location+mediaFile.getFilePath()+mp4_name;//MP4文件路径
        String m3u8_name=mediaFile.getFileId()+".m3u8";//生成的m3u8的文件名
        String m3u8folder_path=video_location+mediaFile.getFilePath()+"hls/";//文件存放路径
        HlsVideoUtil hlsVideoUtil=new HlsVideoUtil(ffmpeg_path,mp4_video_path,m3u8_name,m3u8folder_path);
        String generateM3u8Result = hlsVideoUtil.generateM3u8();
        if (generateM3u8Result==null||!generateM3u8Result.equals("success")){
            //处理失败
            mediaFile.setProcessStatus("303003");
            //定义MediaFileProcess_m3u8
            MediaFileProcess_m3u8 mediaFileProcess_m3u8=new MediaFileProcess_m3u8();
            mediaFileProcess_m3u8.setErrormsg(result);
            mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);//记录失败原因
            mediaFileRepository.save(mediaFile);
            return;
        }
        //处理成功
        mediaFile.setProcessStatus("303002");
        //获取ts文件列表
        List<String> hlsVideoUtil_ts_list = hlsVideoUtil.get_ts_list();
        MediaFileProcess_m3u8 mediaFileProcess_m3u8=new MediaFileProcess_m3u8();
        mediaFileProcess_m3u8.setTslist(hlsVideoUtil_ts_list);
        mediaFile.setMediaFileProcess_m3u8(mediaFileProcess_m3u8);
        //保存fileUrl 视频播放的相对路径
        mediaFile.setFileUrl(mediaFile.getFilePath()+"hls/"+m3u8_name);
        mediaFileRepository.save(mediaFile);


    }
}
