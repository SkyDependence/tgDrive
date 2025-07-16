package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.service.DownloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.CompletableFuture;

/**
 * Description:
 * 文件下载
 * @author SkyDev
 * @date 2025-07-11 17:43:26
 */
@RestController
@RequestMapping("/d")
@Slf4j
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;

    /**
     * Description:
     * 下载文件
     * @param fileID 文件id
     * @author SkyDev
     * @date 2025-07-11 17:43:45
     */
    @GetMapping("/{fileID}")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadFile(@PathVariable String fileID) {
        log.info("接收到下载请求，fileID: " + fileID);
        return CompletableFuture.supplyAsync(() -> downloadService.downloadFile(fileID));
    }

}
