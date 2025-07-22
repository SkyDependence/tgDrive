package com.skydevs.tgdrive.service;

import com.pengrad.telegrambot.model.File;
import com.skydevs.tgdrive.dto.UploadFile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/**
 * 机器人服务类，用于初始化机器人和运行机器人相关服务
 */
public interface BotService{

    /**
     * 获取bot token
     * @return
     */
    String getBotToken();


    /**
     * 根据文件名设置botToken
     * @param filename
     */
    void setBotToken(String filename);


    /**
     * 发送消息
     * @param message
     */
    boolean sendMessage(String message);


    /**
     * 上传文件
     * @param multipartFile
     * @return
     */
    UploadFile getUploadFile(MultipartFile multipartFile, HttpServletRequest request, Long userId);

    /**
     * 获取完整下载路径
     * @param file
     * @return
     */
    String getFullDownloadPath(File file);

    /**
     * 获取自定义URL配置
     * @return 自定义URL，如果没有配置则返回null
     */
    String getCustomUrl();


    /**
     * 根据fileId获取文件
     * @param fileId
     * @return
     */
    File getFile(String fileId);

    /**
     * 根据ID获取文件名
     * @param fileID
     * @return
     */
    String getFileNameByID(String fileID);

    /**
     * 上传文件到Telegram
     * @param inputStream 文件输入流
     * @param path 文件路径
     * @return 文件ID
     */
    String uploadFile(InputStream inputStream, String path);

    /**
     * WebDAV上传
     * @param inputStream
     * @param path
     * @param request WebDAV请求
     * @return fileID
     */
    String uploadFile(InputStream inputStream, String path, HttpServletRequest request);

    /**
     * 从Telegram下载文件
     * @param fileId 文件ID
     * @return 文件输入流
     */
    InputStream downloadFile(String fileId);

    /**
     * 从Telegram删除文件
     * @param fileId 文件ID
     */
    void deleteFile(String fileId);


}
