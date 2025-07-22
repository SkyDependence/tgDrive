package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.result.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;

public interface FileService {
    /**
     * 分页查询文件列表
     * @param page
     * @param size
     * @return
     */
    PageResult getFileList(int page, int size, String keyword, Long userId, String role);

    /**
     * 更新文件url
     * @return
     */
    void updateUrl(HttpServletRequest request);

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

    /**
     * 根据文件ID删除文件
     * @param fileId 文件ID
     */
    void deleteFile(String fileId, Long userId, String role);
    void updateIsPublic(String fileId, boolean isPublic, Long userId, String role);
}
