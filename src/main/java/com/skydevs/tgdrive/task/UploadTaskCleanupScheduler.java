package com.skydevs.tgdrive.task;

import com.skydevs.tgdrive.service.ResumableUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时清理上传任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadTaskCleanupScheduler {

    private final ResumableUploadService resumableUploadService;

    /**
     * 每天凌晨2点执行清理任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTasks() {
        log.info("开始执行上传任务清理...");
        try {
            resumableUploadService.cleanExpiredTasks();
            log.info("上传任务清理完成");
        } catch (Exception e) {
            log.error("上传任务清理失败", e);
        }
    }

    /**
     * 每小时执行一次，清理过期的临时任务
     */
    @Scheduled(fixedDelay = 3600000) // 1小时
    public void cleanupTempTasks() {
        try {
            resumableUploadService.cleanExpiredTasks();
        } catch (Exception e) {
            log.error("临时任务清理失败", e);
        }
    }
}