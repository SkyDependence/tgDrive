package com.skydevs.tgdrive.mapper;

import com.skydevs.tgdrive.entity.WebDavConfig;
import org.apache.ibatis.annotations.*;

/**
 * WebDAV配置Mapper
 */
@Mapper
public interface WebDavConfigMapper {
    
    /**
     * 获取WebDAV配置
     */
    @Select("SELECT * FROM webdav_config WHERE id = 1")
    WebDavConfig getWebDavConfig();
    
    /**
     * 更新WebDAV配置
     */
    @Update("UPDATE webdav_config SET enabled = #{enabled}, path_prefix = #{pathPrefix}, " +
            "require_auth = #{requireAuth}, allowed_roles = #{allowedRoles}, " +
            "max_upload_size = #{maxUploadSize}, allow_mkdir = #{allowMkdir}, " +
            "allow_delete = #{allowDelete}, allow_move = #{allowMove}, " +
            "allow_copy = #{allowCopy}, description = #{description}, " +
            "update_time = #{updateTime} WHERE id = 1")
    int updateWebDavConfig(WebDavConfig config);
    
    /**
     * 插入默认WebDAV配置
     */
    @Insert("INSERT INTO webdav_config (id, enabled, path_prefix, require_auth, " +
            "allowed_roles, max_upload_size, allow_mkdir, allow_delete, allow_move, " +
            "allow_copy, description, create_time, update_time) VALUES " +
            "(1, #{enabled}, #{pathPrefix}, #{requireAuth}, #{allowedRoles}, " +
            "#{maxUploadSize}, #{allowMkdir}, #{allowDelete}, #{allowMove}, " +
            "#{allowCopy}, #{description}, #{createTime}, #{updateTime})")
    int insertWebDavConfig(WebDavConfig config);
    
    /**
     * 检查配置是否存在
     */
    @Select("SELECT COUNT(*) FROM webdav_config WHERE id = 1")
    int checkConfigExists();
}