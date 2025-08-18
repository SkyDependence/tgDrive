package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.dto.WebDavConfig;
import com.skydevs.tgdrive.mapper.WebDavConfigMapper;
import com.skydevs.tgdrive.service.WebDavConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WebDAV配置服务实现类
 */
@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class WebDavConfigServiceImpl implements WebDavConfigService {
    
    private final WebDavConfigMapper webDavConfigMapper;
    
    @Override
    public WebDavConfig getWebDavConfig() {
        WebDavConfig config = webDavConfigMapper.getWebDavConfig();
        if (config == null) {
            // 如果配置不存在，初始化默认配置
            initDefaultConfig();
            config = webDavConfigMapper.getWebDavConfig();
        }
        return config;
    }
    
    @Override
    public boolean updateWebDavConfig(WebDavConfig config) {
        try {
            config.setUpdateTime(System.currentTimeMillis());
            int result = webDavConfigMapper.updateWebDavConfig(config);
            if (result > 0) {
                log.info("WebDAV配置更新成功");
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("更新WebDAV配置失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void initDefaultConfig() {
        try {
            // 检查配置是否已存在
            if (webDavConfigMapper.checkConfigExists() > 0) {
                return;
            }
            
            long currentTime = System.currentTimeMillis();
            WebDavConfig defaultConfig = WebDavConfig.builder()
                    .id(1L)
                    .enabled(true)
                    .requireAuth(true)
                    .allowedRoles("admin")
                    .allowMkdir(true)
                    .allowDelete(true)
                    .allowMove(true)
                    .allowCopy(true)
                    .description("WebDAV服务配置")
                    .createTime(currentTime)
                    .updateTime(currentTime)
                    .build();
                    
            webDavConfigMapper.insertWebDavConfig(defaultConfig);
            log.info("WebDAV默认配置初始化完成");
        } catch (Exception e) {
            log.error("初始化WebDAV默认配置失败: {}", e.getMessage(), e);
        }
    }
    
    @Override
    public boolean isWebDavEnabled() {
        try {
            WebDavConfig config = getWebDavConfig();
            return config != null && Boolean.TRUE.equals(config.getEnabled());
        } catch (Exception e) {
            log.error("检查WebDAV启用状态失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean hasWebDavPermission(String userRole) {
        try {
            WebDavConfig config = getWebDavConfig();
            if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
                return false;
            }
            
            String allowedRoles = config.getAllowedRoles();
            if ("all".equals(allowedRoles)) {
                return true;
            }
            
            return allowedRoles != null && allowedRoles.contains(userRole);
        } catch (Exception e) {
            log.error("检查WebDAV权限失败: {}", e.getMessage(), e);
            return false;
        }
    }
}