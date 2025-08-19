package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.service.DownloadService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/d")
@Slf4j
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadService downloadService;

    //TODO: 断点续传
    /**
     * Description:
     * 根据文件ID下载文件
     * @author SkyDev
     * @date 2025-07-30 16:47:18
     * @param fileID 文件ID
     * @return 文件
     */
    @GetMapping("/{fileID}")
    public CompletableFuture<ResponseEntity<StreamingResponseBody>> downloadFile(@NotBlank(message = "fileID不能为空") @PathVariable String fileID) {
        log.info("接收到下载请求，fileID: " + fileID);
        return CompletableFuture.supplyAsync(() -> downloadService.downloadFile(fileID));
    }

}
