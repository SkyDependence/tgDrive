package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.entity.WebDavConfig;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.WebDavConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * WebDAV配置控制器
 */
@RestController
@RequestMapping("/api/webdav-config")
@Slf4j
public class WebDavConfigController {
    
    @Autowired
    private WebDavConfigService webDavConfigService;
    
    /**
     * 获取WebDAV配置
     */
    @GetMapping
    public Result<WebDavConfig> getWebDavConfig() {
        try {
            WebDavConfig config = webDavConfigService.getWebDavConfig();
            return Result.success(config);
        } catch (Exception e) {
            log.error("获取WebDAV配置失败: {}", e.getMessage(), e);
            return Result.error("获取WebDAV配置失败");
        }
    }
    
    /**
     * 更新WebDAV配置
     */
    @PutMapping
    public Result<Void> updateWebDavConfig(@RequestBody WebDavConfig config) {
        try {
            boolean success = webDavConfigService.updateWebDavConfig(config);
            if (success) {
                return Result.success();
            } else {
                return Result.error("更新WebDAV配置失败");
            }
        } catch (Exception e) {
            log.error("更新WebDAV配置失败: {}", e.getMessage(), e);
            return Result.error("更新WebDAV配置失败");
        }
    }
    
    /**
     * 获取WebDAV状态
     */
    @GetMapping("/status")
    public Result<Boolean> getWebDavStatus() {
        try {
            boolean enabled = webDavConfigService.isWebDavEnabled();
            return Result.success(enabled);
        } catch (Exception e) {
            log.error("获取WebDAV状态失败: {}", e.getMessage(), e);
            return Result.error("获取WebDAV状态失败");
        }
    }
    
    /**
     * 重置为默认配置
     */
    @PostMapping("/reset")
    public Result<Void> resetToDefault() {
        try {
            webDavConfigService.initDefaultConfig();
            return Result.success();
        } catch (Exception e) {
            log.error("重置WebDAV配置失败: {}", e.getMessage(), e);
            return Result.error("重置WebDAV配置失败");
        }
    }
}