package com.skydevs.tgdrive.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public interface DownloadService {
    /**
     * 下载文件
     * @param fileID
     * @return
     */
    ResponseEntity<StreamingResponseBody> downloadFile(String fileID);

    /**
     * 下载/预览文件
     * @param fileID 文件ID
     * @param inline true 时对可预览类型返回 Content-Disposition: inline（浏览器内联渲染，如 PDF 预览）；
     *               false 时返回 attachment（强制下载）
     * @return 文件流
     */
    ResponseEntity<StreamingResponseBody> downloadFile(String fileID, boolean inline);

    /**
     * 校验下载权限：公开文件放行；私有文件要求登录且为属主或管理员
     * @param fileID 文件ID
     */
    void checkDownloadPermission(String fileID);
}
