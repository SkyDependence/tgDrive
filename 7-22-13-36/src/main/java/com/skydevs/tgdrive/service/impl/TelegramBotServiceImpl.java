package com.skydevs.tgdrive.service.impl;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.pengrad.telegrambot.response.SendResponse;
import com.skydevs.tgdrive.exception.BotNotSetException;
import com.skydevs.tgdrive.exception.NoConfigException;
import com.skydevs.tgdrive.service.TelegramBotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Telegram Bot服务实现类
 */
@Service
@Slf4j
public class TelegramBotServiceImpl implements TelegramBotService {

    private String botToken;
    private String chatId;
    private String customUrl;
    private TelegramBot bot;

    @Override
    public void initializeBot(String botToken, String chatId, String customUrl) {
        this.botToken = botToken;
        this.chatId = chatId;
        this.customUrl = customUrl;
        this.bot = new TelegramBot(botToken);
        log.info("Telegram Bot 初始化成功");
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getChatId() {
        return chatId;
    }

    @Override
    public String getCustomUrl() {
        return customUrl;
    }

    @Override
    public Message sendDocument(byte[] fileData, String filename) {
        checkBotInitialized();
        
        int retryCount = 3;
        int baseDelay = 1000;
        
        for (int i = 0; i < retryCount; i++) {
            try {
                SendDocument sendDocument = new SendDocument(chatId, fileData).fileName(filename);
                SendResponse response = bot.execute(sendDocument);
                
                if (response != null && response.isOk() && response.message() != null) {
                    return response.message();
                }
                
                int exponentialDelay = baseDelay * (int)Math.pow(2, i);
                log.warn("发送文档失败，正在准备第{}次重试，等待{}毫秒", (i+1), exponentialDelay);
                Thread.sleep(exponentialDelay);
            } catch (Exception e) {
                if (i == retryCount - 1) {
                    log.error("发送文档失败，已达到最大重试次数: {}", e.getMessage());
                    throw new RuntimeException("发送文档失败，已达到最大重试次数", e);
                }
                try {
                    Thread.sleep(baseDelay * (int)Math.pow(2, i));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("重试等待被中断", ie);
                }
            }
        }
        
        throw new RuntimeException("发送文档失败，已达到最大重试次数");
    }

    @Override
    public Message sendDocument(InputStream inputStream, String filename) {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192];
            int byteRead;
            while ((byteRead = inputStream.read(data)) != -1) {
                buffer.write(data, 0, byteRead);
            }
            return sendDocument(buffer.toByteArray(), filename);
        } catch (IOException e) {
            log.error("读取输入流失败: {}", e.getMessage());
            throw new RuntimeException("读取输入流失败", e);
        }
    }

    @Override
    public String extractFileId(Message message) {
        if (message == null) {
            return null;
        }

        // 按优先级检查可能的文件类型
        if (message.document() != null) {
            return message.document().fileId();
        } else if (message.sticker() != null) {
            return message.sticker().fileId();
        } else if (message.video() != null) {
            return message.video().fileId();
        } else if (message.photo() != null && message.photo().length > 0) {
            return message.photo()[message.photo().length - 1].fileId(); // 取最后一张（通常是最高分辨率）
        } else if (message.audio() != null) {
            return message.audio().fileId();
        } else if (message.animation() != null) {
            return message.animation().fileId();
        } else if (message.voice() != null) {
            return message.voice().fileId();
        } else if (message.videoNote() != null) {
            return message.videoNote().fileId();
        }

        return null; // 没有找到 fileId
    }

    @Override
    public File getFile(String fileId) {
        checkBotInitialized();
        
        GetFile getFile = new GetFile(fileId);
        try {
            GetFileResponse getFileResponse = bot.execute(getFile);
            return getFileResponse.file();
        } catch (NullPointerException e) {
            log.error("当前未加载配置文件！{}", e.getMessage());
            throw new NoConfigException("当前未加载配置文件！");
        }
    }

    @Override
    public String getFullFilePath(File file) {
        checkBotInitialized();
        return bot.getFullFilePath(file);
    }

    @Override
    public boolean sendMessage(String message) {
        checkBotInitialized();
        
        try {
            bot.execute(new SendMessage(chatId, message));
            log.info("消息发送成功");
            return true;
        } catch (Exception e) {
            log.error("消息发送失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void deleteMessage(String messageId) {
        checkBotInitialized();
        
        try {
            bot.execute(new DeleteMessage(chatId, Integer.parseInt(messageId)));
            log.info("消息删除成功，Message ID: {}", messageId);
        } catch (Exception e) {
            log.error("消息删除失败: {}", e.getMessage());
            throw new RuntimeException("消息删除失败", e);
        }
    }

    @Override
    public boolean isInitialized() {
        return bot != null && botToken != null && chatId != null;
    }

    /**
     * 检查Bot是否已初始化
     */
    private void checkBotInitialized() {
        if (!isInitialized()) {
            throw new BotNotSetException("Telegram Bot 未初始化");
        }
    }
}