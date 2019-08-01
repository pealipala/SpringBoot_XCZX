package com.xuecheng.test.fastdfs;

import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @author Administrator
 * @version 1.0
 **/
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestFastDFS {

    //上传文件
    @Test
    public void uploadFile(){
        try {
            //加载配置文件
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            //创建trackerClient
            TrackerClient trackerClient=new TrackerClient();
            //连接tracker
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取storage
            StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);
            //创建storageClient
            StorageClient storageClient=new StorageClient(trackerServer,storageServer);
            String filePath="C:/Users/叶朝泽/Desktop/图片/20190418181350.jpg";
            //上传文件
            String[] file = storageClient.upload_file(filePath, "png", null);
            System.out.println(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }

    //下载文件
    @Test
    public void downloadFile(){
        try {
            //加载配置文件
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            //创建trackerClient
            TrackerClient trackerClient=new TrackerClient();
            //连接
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取storage
            StorageServer storageServer = trackerClient.getStoreStorage(trackerServer);
            //创建StorageClient
            StorageClient storageClient=new StorageClient(trackerServer,storageServer);
            String group_name="group1";
            String remote_name="M00/00/00/wKjphV0NxoaAHTkiAAAhETWLeYY266.png";
            String local_name="F:/Codes/logos/vue_dem.png";
            //下载文件
            byte[] bytes = storageClient.download_file(group_name, remote_name);
            //使用输出流
            FileOutputStream outputStream=new FileOutputStream(new File(local_name));
            outputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }

}
