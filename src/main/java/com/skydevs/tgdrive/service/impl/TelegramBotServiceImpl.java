package com.skydevs.tgdrive.service.impl;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.File;
import com.pengrad.telegrambot.request.GetFile;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.GetFileResponse;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.exception.bot.BotNotSetException;
import com.skydevs.tgdrive.exception.config.ConfigFileNotFoundException;
import com.skydevs.tgdrive.exception.config.NoConfigException;
import com.skydevs.tgdrive.service.ConfigService;
import com.skydevs.tgdrive.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Telegram Bot服务实现类
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramBotServiceImpl implements TelegramBotService {
    private final ConfigService configService;

    private String botToken;
    private String chatId;
    private String customUrl;
    private TelegramBot bot;

    @Override
    public TelegramBot getBot() {
        return this.bot;
    }

    @Override
    public String getChatId() {
        return this.chatId;
    }

    @Override
    public void initializeBot(String filename) {
        ConfigForm config = configService.get(filename);
        if (config == null) {
            log.error("配置文件不存在");
            throw new ConfigFileNotFoundException();
        }
        this.botToken = config.getToken();
        this.chatId = config.getTarget();
        this.customUrl = config.getUrl();
        this.bot = new TelegramBot(botToken);
        log.info("Telegram Bot 初始化成功");
    }

    @Override
    public String getCustomUrl() {
        return customUrl;
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