package com.skydevs.tgdrive.service.impl;

import com.pengrad.telegrambot.model.File;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.entity.FileInfo;
import com.skydevs.tgdrive.exception.*;
import com.skydevs.tgdrive.mapper.FileMapper;
import com.skydevs.tgdrive.service.BotService;
import com.skydevs.tgdrive.service.ConfigService;
import com.skydevs.tgdrive.service.FileStorageService;
import com.skydevs.tgdrive.service.TelegramBotService;
import com.skydevs.tgdrive.utils.StringUtil;
import com.skydevs.tgdrive.utils.UserFriendly;
import com.skydevs.tgdrive.websocket.UploadProgressWebSocketHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Slf4j
public class BotServiceImpl implements BotService {

    @Autowired
    private ConfigService configService;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private TelegramBotService telegramBotService;
    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private UploadProgressWebSocketHandler uploadProgressWebSocketHandler;
    
    private String customUrl; // 自定义URL配置


    /**
     * 设置基本配置
     *
     * @param filename 配置文件名
     */
    public void setBotToken(String filename) {
        ConfigForm config = configService.get(filename);
        if (config == null) {
            log.error("配置文件不存在");
            throw new ConfigFileNotFoundException();
        }
        try {
            String botToken = config.getToken();
            String chatId = config.getTarget();
            customUrl = config.getUrl(); // 设置自定义URL
            
            // 初始化Telegram Bot服务
            telegramBotService.initializeBot(botToken, chatId, customUrl);
        } catch (Exception e) {
            log.error("获取Bot Token失败: {}", e.getMessage());
            throw new GetBotTokenFailedException();
        }
    }



    /**
     * 生成上传文件
     *
     * @param multipartFile
     * @param request
     * @return
     */
    @Override
    public UploadFile getUploadFile(MultipartFile multipartFile, HttpServletRequest request, Long userId) {
        UploadFile uploadFile = new UploadFile();
        if (!multipartFile.isEmpty()) {
            String downloadUrl = uploadFile(multipartFile, request, userId);
            uploadFile.setFileName(multipartFile.getOriginalFilename());
            uploadFile.setDownloadLink(downloadUrl);
        } else {
            uploadFile.setFileName("文件不存在");
        }

        return uploadFile;
    }

    /**
     * 上传文件
     *
     * @param multipartFile
     * @param request
     * @return 文件下载地址
     */
    private String uploadFile(MultipartFile multipartFile, HttpServletRequest request, Long userId) {
        try {
            // 优先使用自定义URL，如果没有配置则使用请求中的URL
            String prefix = (customUrl != null && !customUrl.trim().isEmpty()) ? customUrl.trim() : StringUtil.getPrefix(request);
            InputStream inputStream = multipartFile.getInputStream();
            String filename = multipartFile.getOriginalFilename();
            long size = multipartFile.getSize();
            
            // 使用FileStorageService上传文件
            String fileID = fileStorageService.uploadFile(inputStream, filename, size);
            
            // 保存文件信息到数据库
            FileInfo fileInfo = FileInfo.builder()
                    .fileId(fileID)
                    .size(UserFriendly.humanReadableFileSize(size))
                    .fullSize(size)
                    .uploadTime(LocalDateTime.now(ZoneOffset.UTC).toEpochSecond(ZoneOffset.UTC))
                    .downloadUrl(prefix + "/d/" + fileID)
                    .fileName(filename)
                    .userId(userId)
                    .build();
            fileMapper.insertFile(fileInfo);
            
            return prefix + "/d/" + fileID;
        } catch (IOException e) {
            log.error("文件上传失败，响应信息：{}", e.getMessage());
            throw new RuntimeException("文件上传失败");
        }
    }

    /**
     * 获取完整下载路径
     *
     * @param file
     * @return
     */
    public String getFullDownloadPath(File file) {
        String fullPath = telegramBotService.getFullFilePath(file);
        log.info("获取完整的下载路径: {}", fullPath);
        return fullPath;
    }

    @Override
    public String getCustomUrl() {
        return telegramBotService.getCustomUrl();
    }

    /**
     * 根据fileId获取文件
     *
     * @param fileId
     * @return
     */
    public File getFile(String fileId) {
        return telegramBotService.getFile(fileId);
    }

    /**
     * 根据文件id获取文件名
     *
     * @param fileID
     * @return
     */
    @Override
    public String getFileNameByID(String fileID) {
        File file = telegramBotService.getFile(fileID);
        return file.filePath();
    }

    /**
     * 发送消息
     *
     * @param m
     */
    public boolean sendMessage(String m) {
        return telegramBotService.sendMessage(m);
    }


    /**
     * 获取bot token
     *
     * @return
     */
    @Override
    public String getBotToken() {
        return telegramBotService.getBotToken();
    }

    /**
     * 上传文件
     * @param inputStream 文件输入流
     * @param path 文件路径
     * @return
     */
    @Override
    public String uploadFile(InputStream inputStream, String path) {
        try {
            String filename = path.substring(path.lastIndexOf('/') + 1);
            long size = inputStream.available();
            return fileStorageService.uploadFile(inputStream, filename, size);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public String uploadFile(InputStream inputStream, String path, HttpServletRequest request) {
        try {
            String filename = path.substring(path.lastIndexOf('/') + 1);
            long size = request.getContentLengthLong();
            return fileStorageService.uploadFile(inputStream, filename, size);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public InputStream downloadFile(String fileId) {
        return fileStorageService.downloadFile(fileId);
    }

    @Override
    public void deleteFile(String fileId) {
        fileStorageService.deleteFile(fileId);
    }


}