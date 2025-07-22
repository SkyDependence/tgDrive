package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.MiscService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 杂项管理控制器
 * 处理背景图片设置和SEO配置
 */
@Slf4j
@RestController
@RequestMapping("/api/misc")
@RequiredArgsConstructor
public class MiscController {

    private final MiscService miscService;

    /**
     * 获取所有杂项设置
     */
    @GetMapping("/settings")
    public Result<Map<String, Object>> getSettings() {
        try {
            Map<String, Object> settings = miscService.getAllSettings();
            return Result.success(settings);
        } catch (Exception e) {
            log.error("获取杂项设置失败", e);
            return Result.error("获取设置失败: " + e.getMessage());
        }
    }

    // 图片上传功能已移除，现在使用图片链接方式 - STlegend 2025-7-22-10-28

    /**
     * 保存背景设置
     */
    @PostMapping("/background")
    public Result<Void> saveBackgroundSettings(
            @RequestBody Map<String, Object> backgroundSettings) {
        try {
            miscService.saveBackgroundSettings(backgroundSettings);
            return Result.success(null);
        } catch (Exception e) {
            log.error("保存背景设置失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 保存SEO设置
     */
    @PostMapping("/seo")
    public Result<Void> saveSeoSettings(
            @RequestBody Map<String, Object> seoSettings) {
        try {
            miscService.saveSeoSettings(seoSettings);
            return Result.success(null);
        } catch (Exception e) {
            log.error("保存SEO设置失败", e);
            return Result.error("保存失败: " + e.getMessage());
        }
    }

    /**
     * 获取背景设置
     */
    @GetMapping("/background")
    public Result<Map<String, Object>> getBackgroundSettings() {
        try {
            Map<String, Object> settings = miscService.getBackgroundSettings();
            return Result.success(settings);
        } catch (Exception e) {
            log.error("获取背景设置失败", e);
            return Result.error("获取设置失败: " + e.getMessage());
        }
    }

    /**
     * 获取SEO设置
     */
    @GetMapping("/seo")
    public Result<Map<String, Object>> getSeoSettings() {
        try {
            Map<String, Object> settings = miscService.getSeoSettings();
            return Result.success(settings);
        } catch (Exception e) {
            log.error("获取SEO设置失败", e);
            return Result.error("获取设置失败: " + e.getMessage());
        }
    }
}