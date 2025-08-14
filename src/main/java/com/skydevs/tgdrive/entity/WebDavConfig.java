package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebDAV配置实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebDavConfig {
    /**
     * 配置ID
     */
    private Long id;
    
    /**
     * WebDAV是否启用
     */
    private Boolean enabled;

    /**
     * 是否需要认证
     */
    private Boolean requireAuth;
    
    /**
     * 允许的用户角色（admin/visitor/all）
     */
    private String allowedRoles;

    /**
     * 是否允许创建目录
     */
    private Boolean allowMkdir;
    
    /**
     * 是否允许删除文件
     */
    private Boolean allowDelete;
    
    /**
     * 是否允许移动/重命名文件
     */
    private Boolean allowMove;
    
    /**
     * 是否允许复制文件
     */
    private Boolean allowCopy;
    
    /**
     * 配置描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private Long createTime;
    
    /**
     * 更新时间
     */
    private Long updateTime;
}