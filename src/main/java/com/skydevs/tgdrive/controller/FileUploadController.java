package com.skydevs.tgdrive.controller;

import com.alibaba.fastjson.JSON;
import com.skydevs.tgdrive.dto.Message;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.service.BotService;
import com.skydevs.tgdrive.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RestController
@Slf4j
@RequestMapping("/api")
public class FileUploadController {

    @Autowired
    private BotService botService;

    /**
     * 加载配置
     * @param filename
     * @return
     */
    @GetMapping("/config/{filename}")
    public ResponseEntity<String> loadConfig(@PathVariable("filename") String filename) {
        botService.setBotToken(filename);
        log.info("加载配置成功");
        return ResponseEntity.ok("加载配置成功");
    }

    /**
     * 上传文件
     * @param multipartFiles
     * @return
     */
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file")MultipartFile[] multipartFiles, HttpServletRequest request) {
        if (multipartFiles.length == 0 || multipartFiles == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("上传的文件为空");
        }

        List<UploadFile> uploadFiles = new ArrayList<>();

        for (MultipartFile file : multipartFiles) {
            UploadFile uploadFile = new UploadFile();
            if (!file.isEmpty()) {
                String downloadPath = botService.uploadFile(file);
                String protocol = request.getScheme(); // 获取协议 http 或 https
                String host = request.getServerName(); // 获取主机名 localhost 或实际域名
                int port = request.getServerPort(); // 获取端口号 8080 或其他
                String downloadUrl = protocol + "://" + host + ":" + port + downloadPath;
                uploadFile.setFileName(file.getOriginalFilename());
                uploadFile.setDownloadLink(downloadUrl);
                uploadFiles.add(uploadFile);
            } else {
                uploadFile.setFileName("文件不存在");
            }
        }

        String resultJSON = JSON.toJSONString(uploadFiles);
        return ResponseEntity.ok(resultJSON);

    }

    /**
     * 发送消息
     * @param message
     * @return
     */
    @PostMapping("/send-message")
    public ResponseEntity<String> sendMessage(@RequestBody Message message){
        log.info("处理消息发送");
        botService.sendMessage(message.getMessage());
        return ResponseEntity.ok("发送成功");
    }
}
