package com.xuecheng.manage_media.controller;

import com.xuecheng.api.media.MediaUploadControllerApi;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.service.MediaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media/upload")
public class MediaUploadController implements MediaUploadControllerApi {

    @Autowired
    private MediaService mediaService;


    @Override
    @PostMapping("/register")
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        return mediaService.register(fileMd5, fileName, fileSize, mimetype, fileExt);
    }

    @Override
    @PostMapping("/checkChunk")
    public CheckChunkResult checkChunk(String fileMd5, Integer chunk, Integer chunkSize) {
        return mediaService.checkChunk(fileMd5, chunk, chunkSize);
    }

    @Override
    @PostMapping("/uploadChunk")
    public ResponseResult uploadChunk(MultipartFile file, String fileMd5, Integer chunk) {
        return mediaService.uploadChunk(file, fileMd5, chunk);
    }

    @Override
    @PostMapping("/mergeChunks")
    public ResponseResult mergeChunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        return mediaService.mergeChunks(fileMd5, fileName, fileSize, mimetype, fileExt);
    }
}
