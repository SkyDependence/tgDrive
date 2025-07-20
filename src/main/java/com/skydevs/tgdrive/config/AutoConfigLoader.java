package com.skydevs.tgdrive.config;

import com.skydevs.tgdrive.dto.ConfigForm;
import com.skydevs.tgdrive.service.BotService;
import com.skydevs.tgdrive.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自动配置加载器
 * 在应用启动时自动加载最新的配置
 */
@Component
@Slf4j
public class AutoConfigLoader implements CommandLineRunner {

    @Autowired
    private ConfigService configService;
    
    @Autowired
    private BotService botService;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("开始自动加载配置...");
            
            // 获取所有配置
            List<ConfigForm> configForms = configService.getForms();
            
            if (configForms != null && !configForms.isEmpty()) {
                // 选择第一个可用的配置
                ConfigForm selectedConfig = configForms.get(0);
                
                // 如果有多个配置，优先选择名称包含"default"或"main"的配置
                for (ConfigForm config : configForms) {
                    String configName = config.getName().toLowerCase();
                    if (configName.contains("default") || configName.contains("main")) {
                        selectedConfig = config;
                        break;
                    }
                }
                
                // 自动加载选中的配置
                botService.setBotToken(selectedConfig.getName());
                
                log.info("自动加载配置成功: {} (共找到{}个配置)", selectedConfig.getName(), configForms.size());
                
                // 如果有多个配置，提示用户
                if (configForms.size() > 1) {
                    log.info("检测到多个配置文件，已自动选择: {}。如需切换配置，请使用管理界面手动加载。", selectedConfig.getName());
                }
            } else {
                log.warn("未找到任何配置文件，请通过管理界面添加配置");
            }
            
        } catch (Exception e) {
            log.warn("自动加载配置失败: {}", e.getMessage());
            log.info("应用将正常启动，请通过管理界面手动加载配置");
        }
    }
}