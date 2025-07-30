package com.skydevs.tgdrive.service;

import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Message;

import java.io.InputStream;

/**
 * Telegram Bot服务接口
 * 专门处理与Telegram Bot API相关的操作
 */
public interface TelegramBotService {

    /**
     * 初始化Bot配置
     */
    void initializeBot(String filename);

    /**
     * 获取自定义URL
     * @return 自定义URL
     */
    String getCustomUrl();

    /**
     * 发送文档到Telegram
     * @param fileData 文件数据
     * @param filename 文件名
     * @return 消息对象
     */
    Message sendDocument(byte[] fileData, String filename);

    /**
     * 发送文档到Telegram
     * @param inputStream 文件输入流
     * @param filename 文件名
     * @return 消息对象
     */
    Message sendDocument(InputStream inputStream, String filename);

    /**
     * 根据文件ID获取文件信息
     * @param fileId 文件ID
     * @return 文件信息
     */
    File getFile(String fileId);

    /**
     * 获取文件完整下载路径
     * @param file 文件对象
     * @return 完整下载路径
     */
    String getFullFilePath(File file);

    /**
     * 发送消息
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(String message);

    /**
     * 删除消息
     * @param messageId 消息ID
     */
    void deleteMessage(String messageId);

    /**
     * 检查Bot是否已初始化
     * @return 是否已初始化
     */
    boolean isInitialized();
}