package com.skydevs.tgdrive.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;

/**
 * Telegram Bot服务接口
 * 专门处理与Telegram Bot API相关的操作
 */
public interface TelegramBotService {

    /**
     * Description:
     * 获取bot
     * @author SkyDev
     * @date 2025-08-01 17:24:28
     * @return bot
     */
    TelegramBot getBot();

    /**
     * Description:
     * 获取chatId
     * @author SkyDev
     * @date 2025-08-12 14:00:00
     * @return chatId
     */
    String getChatId();

    /**
     * 初始化Bot配置
     */
    void initializeBot(String name);

    /**
     * 获取自定义URL
     * @return 自定义URL
     */
    String getCustomUrl();

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
     * 检查Bot是否已初始化
     * @return 是否已初始化
     */
    boolean isInitialized();


    /**
     * 删除文件
     * @param fileId 文件ID
     */
    void deleteFile(Integer fileId);
}