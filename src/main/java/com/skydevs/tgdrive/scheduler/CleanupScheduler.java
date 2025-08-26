package com.skydevs.tgdrive.scheduler;

import com.skydevs.tgdrive.service.impl.ChunkStorageServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务调度器
 * 用于清理过期的上传会话
 */
@Slf4j
@Component
public class CleanupScheduler {

    @Autowired
    private ChunkStorageServiceImpl chunkStorageServiceImpl;

    /**
     * 每天凌晨 3 点执行清理任务
     * 清理 7 天前未完成的上传会话
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredUploadSessions() {
        try {
            log.info("Starting cleanup of expired upload sessions...");
            int cleanedCount = chunkStorageServiceImpl.cleanupExpiredSessions();
            log.info("Cleanup completed. Cleaned {} expired sessions", cleanedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup expired upload sessions", e);
        }
    }
}