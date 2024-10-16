package com.skydevs.tgdrive.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skydevs.tgdrive.config.AppConfig;
import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.exception.FileNotFoundException;
import com.skydevs.tgdrive.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    @Value("${server.port}")
    private int serverPort;

    private final File configDir = new File("configJSON");

    /**
     * 获取配置文件
     * @param filename
     * @return
     */
    @Override
    public AppConfig get(String filename) {

        File configFile = new File(configDir, filename + ".json");
        if (configFile.exists()) {
            try {
                String content = new String(Files.readAllBytes(Paths.get(configFile.toString())));
                return JSON.parseObject(content, AppConfig.class);
            } catch (Exception e) {
                System.err.println("配置文件读取失败: " + e.getMessage());
                throw new RuntimeException("配置读取失败");
            }
        } else {
            throw new FileNotFoundException("文件不存在");
        }
    }

    /**
     * 保存配置文件
     * @param configForm
     */
    @Override
    public void save(ConfigForm configForm) {
        /*
        if (configForm.getUrl() == null || configForm.getUrl().isEmpty()) {
            configForm.setUrl("localhost:" + serverPort);
        }

         */
        if (!configDir.exists()) {
            if (!configDir.mkdirs()) {
                throw new RuntimeException("无法创建 configJSON 文件夹");
            }
        }

        File configFile = new File(configDir, configForm.getName() + ".json");

        try {
            String jsonString = JSON.toJSONString(configForm, true);
            Files.write(Paths.get(configFile.toURI()),jsonString.getBytes());
        } catch (IOException e) {
            log.error("保存配置文件失败：" + e.getMessage());
            throw new RuntimeException("配置保存失败", e);
        }
        //TODO 将用JSON保存的形式转为用sqlite存储
    }
}
