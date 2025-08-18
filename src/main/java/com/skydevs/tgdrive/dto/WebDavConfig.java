package com.skydevs.tgdrive.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    @NotNull
    private Long id;
    
    /**
     * WebDAV是否启用
     */
    @NotNull
    private Boolean enabled;

    /**
     * 是否需要认证
     */
    @NotNull
    private Boolean requireAuth;
    
    /**
     * 允许的用户角色（admin/visitor/all）
     */
    @NotBlank
    private String allowedRoles;

    /**
     * 是否允许创建目录
     */
    @NotNull
    private Boolean allowMkdir;
    
    /**
     * 是否允许删除文件
     */
    @NotNull
    private Boolean allowDelete;
    
    /**
     * 是否允许移动/重命名文件
     */
    @NotNull
    private Boolean allowMove;
    
    /**
     * 是否允许复制文件
     */
    @NotNull
    private Boolean allowCopy;
    
    /**
     * 配置描述
     */
    @NotNull
    private String description;
    
    /**
     * 创建时间
     */
    @NotNull
    private Long createTime;
    
    /**
     * 更新时间
     */
    @NotNull
    private Long updateTime;
}