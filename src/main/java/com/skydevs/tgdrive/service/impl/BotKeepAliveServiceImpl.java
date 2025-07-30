package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.service.BotKeepAliveService;
import com.skydevs.tgdrive.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 机器人保活服务实现类
 * 通过定时发送消息来保持机器人活跃状态
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BotKeepAliveServiceImpl implements BotKeepAliveService {
    
    private final TelegramBotService telegramBotService;
    
    private final AtomicBoolean isTaskRunning = new AtomicBoolean(true);
    
    /**
     * 每天凌晨2点发送保活消息
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledKeepAlive() {
        if (isTaskRunning.get()) {
            log.info("开始执行定时保活任务");
            boolean success = sendKeepAliveMessage();
            if (success) {
                log.info("定时保活消息发送成功");
            } else {
                log.warn("定时保活消息发送失败");
            }
        }
    }
    
    @Override
    public boolean sendKeepAliveMessage() {
        try {
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String message = String.format(
                "🤖 TG-Drive 机器人保活消息\n" +
                "⏰ 时间: %s\n" +
                "✅ 系统运行正常\n" +
                "📊 这是一条自动发送的保活消息，用于保持机器人活跃状态。",
                currentTime
            );
            
            log.info("发送保活消息: {}", message);
            
            // 使用现有的BotService发送消息
            return telegramBotService.sendMessage(message);
            
        } catch (Exception e) {
            log.error("发送保活消息失败", e);
            return false;
        }
    }
    

    
    @Override
    public void startKeepAliveTask() {
        isTaskRunning.set(true);
        log.info("机器人保活任务已启动");
    }
    
    @Override
    public void stopKeepAliveTask() {
        isTaskRunning.set(false);
        log.info("机器人保活任务已停止");
    }
    
    @Override
    public boolean isKeepAliveTaskRunning() {
        return isTaskRunning.get();
    }
}