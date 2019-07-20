package com.cxhello.gmall.manage.controller;

import org.apache.commons.lang3.StringUtils;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * @author CaiXiaoHui
 * @create 2019-07-04 14:33
 */
@RestController
@CrossOrigin
public class FileUploadController {

    @Value("${fileServer.url}") //该注解必须被spring容器扫描,
    String fileUrl;


    //文件上传
    @RequestMapping("fileUpload")
    public String fileUpload(MultipartFile file) throws IOException, MyException {
        String imgUrl = fileUrl;
        if (file != null) {
            //读取配置文件
            String configFile = this.getClass().getResource("/tracker.conf").getFile();
            ClientGlobal.init(configFile);
            //客户端
            TrackerClient trackerClient = new TrackerClient();
            //通过客户端打开连接
            TrackerServer trackerServer = trackerClient.getConnection();
            //存储客户端
            StorageClient storageClient = new StorageClient(trackerServer, null);
            String originalFilename = file.getOriginalFilename();
            //获取文件后缀名
            String exName = StringUtils.substringAfterLast(originalFilename, ".");
            //    usr/bin/fdfs_test /etc/fdfs/client.conf upload /root/001.jpg
            String[] upload_file = storageClient.upload_file(file.getBytes(), exName, null);
            for (int i = 0; i < upload_file.length; i++) {
                String path = upload_file[i];

                imgUrl +=  "/" + path;
            }
        }
            return imgUrl;
    }
}
