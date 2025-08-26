package com.skydevs.tgdrive.service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;

public interface DownloadService {
    /**
     * 下载文件
     * @param fileID
     * @return
     */
    ResponseEntity<StreamingResponseBody> downloadFile(String fileID);

    /**
     * 下载文件并返回输入流
     * @param fileID
     * @return
     * @throws IOException
     */
    InputStream downloadFileInputStream(String fileID) throws IOException;
}
