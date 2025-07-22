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
}
