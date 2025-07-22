package com.skydevs.tgdrive.config;

import com.skydevs.tgdrive.service.WebDavConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * WebDAV配置初始化器
 * 在应用启动时初始化WebDAV默认配置
 */
@Component
@Slf4j
public class WebDavConfigInitializer implements ApplicationRunner {
    
    @Autowired
    private WebDavConfigService webDavConfigService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            log.info("开始初始化WebDAV配置...");
            webDavConfigService.initDefaultConfig();
            log.info("WebDAV配置初始化完成");
        } catch (Exception e) {
            log.error("WebDAV配置初始化失败: {}", e.getMessage(), e);
        }
    }
}