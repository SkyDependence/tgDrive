package com.skydevs.tgdrive.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

/**
 * 杂项管理服务
 * 处理背景图片和SEO设置的存储与管理
 */
@Service
public interface MiscService {
    /**
     * 获取所有杂项设置
     */
    Map<String, Object> getAllSettings();

    // 图片上传功能已移除，现在使用图片链接方式

    /**
     * 保存背景设置
     */
    void saveBackgroundSettings(Map<String, Object> settings) throws IOException;

    /**
     * 保存SEO设置
     */
    void saveSeoSettings(Map<String, Object> settings) throws IOException;

    /**
     * 获取背景设置
     */
    Map<String, Object> getBackgroundSettings();

    /**
     * 获取SEO设置
     */
    Map<String, Object> getSeoSettings();

    /**
     * 清理旧的背景图片文件
     */
    void cleanupOldBackgroundImages(String currentImageUrl);
}