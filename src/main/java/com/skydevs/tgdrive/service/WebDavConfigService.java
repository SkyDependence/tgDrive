package com.skydevs.tgdrive.service;

import com.skydevs.tgdrive.dto.WebDavConfig;

/**
 * WebDAV配置服务接口
 */
public interface WebDavConfigService {
    
    /**
     * 获取WebDAV配置
     * @return WebDAV配置
     */
    WebDavConfig getWebDavConfig();
    
    /**
     * 更新WebDAV配置
     * @param config WebDAV配置
     * @return 是否成功
     */
    boolean updateWebDavConfig(WebDavConfig config);
    
    /**
     * 初始化默认WebDAV配置
     */
    void initDefaultConfig();
    
    /**
     * 检查WebDAV是否启用
     * @return 是否启用
     */
    boolean isWebDavEnabled();
    
    /**
     * 检查用户是否有WebDAV访问权限
     * @param userRole 用户角色
     * @return 是否有权限
     */
    boolean hasWebDavPermission(String userRole);
}