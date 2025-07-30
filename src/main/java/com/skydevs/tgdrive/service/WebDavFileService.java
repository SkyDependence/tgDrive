package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.FileInfo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

public interface WebDavFileService {

    /**
     * 上传文件到Telegram
     *
     * @param inputStream 文件输入流
     * @param request
     * @return 文件ID
     */
    String uploadByWebDav(InputStream inputStream, HttpServletRequest request);

    /**
     * WebDAV下载文件
     * @param path 文件路径
     * @return 文件流
     */
    ResponseEntity<StreamingResponseBody> downloadByWebDav(String path);

    /**
     * 从Telegram删除文件
     * @param path 文件路径
     */
    void deleteByWebDav(String path);

    /**
     * 获取文件列表
     *
     * @param path 路径
     * @return 文件列表
     */
    List<FileInfo> listFiles(String path);
}
