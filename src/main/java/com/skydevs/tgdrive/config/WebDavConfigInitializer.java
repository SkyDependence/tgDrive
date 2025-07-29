package com.skydevs.tgdrive.config;

import com.skydevs.tgdrive.service.WebDavConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * WebDAV配置初始化器
 * 在应用启动时初始化WebDAV默认配置
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WebDavConfigInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final WebDavConfigService webDavConfigService;

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        try {
            log.info("开始初始化WebDAV配置...");
            webDavConfigService.initDefaultConfig();
            log.info("WebDAV配置初始化完成");
        } catch (Exception e) {
            log.error("WebDAV配置初始化失败: {}", e.getMessage(), e);
        }
    }
}