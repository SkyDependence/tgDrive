package com.skydevs.tgdrive.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skydevs.tgdrive.service.MiscService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiscServiceImpl implements MiscService {

    private final ObjectMapper objectMapper;

    @Value("${app.upload.path:uploads}")
    private String uploadPath;

    @Value("${server.port:8085}")
    private String serverPort;

    private static final String SETTINGS_DIR = "settings";
    private static final String BACKGROUND_SETTINGS_FILE = "background.json";
    private static final String SEO_SETTINGS_FILE = "seo.json";
    private static final String BACKGROUND_UPLOAD_DIR = "backgrounds";

    /**
     * 获取所有杂项设置
     */
    @Override
    public Map<String, Object> getAllSettings() {
        Map<String, Object> allSettings = new HashMap<>();
        allSettings.put("background", getBackgroundSettings());
        allSettings.put("seo", getSeoSettings());
        return allSettings;
    }

    // 图片上传功能已移除，现在使用图片链接方式

    /**
     * 保存背景设置
     */
    @Override
    public void saveBackgroundSettings(Map<String, Object> settings) throws IOException {
        Path settingsDir = Paths.get(uploadPath, SETTINGS_DIR);
        Files.createDirectories(settingsDir);

        Path settingsFile = settingsDir.resolve(BACKGROUND_SETTINGS_FILE);
        objectMapper.writeValue(settingsFile.toFile(), settings);

        log.info("背景设置已保存: {}", settings);
    }

    /**
     * 保存SEO设置
     */
    @Override
    public void saveSeoSettings(Map<String, Object> settings) throws IOException {
        Path settingsDir = Paths.get(uploadPath, SETTINGS_DIR);
        Files.createDirectories(settingsDir);

        Path settingsFile = settingsDir.resolve(SEO_SETTINGS_FILE);
        objectMapper.writeValue(settingsFile.toFile(), settings);

        log.info("SEO设置已保存: {}", settings);
    }

    /**
     * 获取背景设置
     */
    @Override
    public Map<String, Object> getBackgroundSettings() {
        try {
            Path settingsFile = Paths.get(uploadPath, SETTINGS_DIR, BACKGROUND_SETTINGS_FILE);
            if (Files.exists(settingsFile)) {
                return objectMapper.readValue(
                        settingsFile.toFile(),
                        new TypeReference<Map<String, Object>>() {}
                );
            }
        } catch (IOException e) {
            log.error("读取背景设置失败", e);
        }

        // 返回默认设置
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("enabled", false);
        defaultSettings.put("imageUrl", "");
        defaultSettings.put("mode", "cover");
        defaultSettings.put("opacity", 80);
        return defaultSettings;
    }

    /**
     * 获取SEO设置
     */
    @Override
    public Map<String, Object> getSeoSettings() {
        try {
            Path settingsFile = Paths.get(uploadPath, SETTINGS_DIR, SEO_SETTINGS_FILE);
            if (Files.exists(settingsFile)) {
                return objectMapper.readValue(
                        settingsFile.toFile(),
                        new TypeReference<Map<String, Object>>() {}
                );
            }
        } catch (IOException e) {
            log.error("读取SEO设置失败", e);
        }

        // 返回默认设置
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("title", "TG-Drive - Stanley_Legend");
        defaultSettings.put("description", "基于Telegram的云存储解决方案");
        defaultSettings.put("keywords", "TG-Drive,Telegram,云存储,文件管理,Stanley_Legend");
        defaultSettings.put("author", "Stanley_Legend");
        defaultSettings.put("favicon", "/favicon.ico");
        defaultSettings.put("ogImage", "/favicon.ico");
        return defaultSettings;
    }

    /**
     * 清理旧的背景图片文件
     */
    @Override
    public void cleanupOldBackgroundImages(String currentImageUrl) {
        try {
            Path backgroundDir = Paths.get(uploadPath, BACKGROUND_UPLOAD_DIR);
            if (!Files.exists(backgroundDir)) {
                return;
            }

            Files.list(backgroundDir)
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        String filename = path.getFileName().toString();
                        String currentFilename = currentImageUrl != null ?
                                currentImageUrl.substring(currentImageUrl.lastIndexOf("/") + 1) : "";
                        return !filename.equals(currentFilename) && filename.startsWith("bg_");
                    })
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                            log.info("已删除旧背景图片: {}", path.getFileName());
                        } catch (IOException e) {
                            log.warn("删除旧背景图片失败: {}", path.getFileName(), e);
                        }
                    });
        } catch (IOException e) {
            log.error("清理旧背景图片失败", e);
        }
    }
}
