package com.skydevs.tgdrive.mapper;

import com.github.pagehelper.Page;
import com.skydevs.tgdrive.entity.FileInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FileMapper {

    /**
     * 插入已上传文件
     * @param fileInfo
     */
    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir) VALUES (#{fileName}, #{downloadUrl}, #{uploadTime}, #{fileId}, #{size}, #{fullSize}, #{webdavPath}, #{dir})")
    void insertFile(FileInfo fileInfo);

    /**
     * 获取全部文件
     * @return
     */
    @Select("SELECT * FROM files order by upload_time desc ")
    Page<FileInfo> getAllFiles();

    @SelectProvider(type = FileSqlProvider.class, method = "getFilteredFilesQuery")
    Page<FileInfo> getFilteredFiles(@Param("keyword") String keyword, @Param("userId") Long userId, @Param("role") String role);

    class FileSqlProvider {
        public String getFilteredFilesQuery(String keyword, Long userId, String role) {
            StringBuilder sql = new StringBuilder("SELECT * FROM files WHERE 1=1");
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND file_name LIKE '%").append(keyword).append("%'");
            }
            if ("user".equals(role)) {
                sql.append(" AND user_id = ").append(userId);
            } else if ("visitor".equals(role)) {
                sql.append(" AND is_public = 1");
            }
            sql.append(" ORDER BY upload_time DESC");
            return sql.toString();
        }
    }

    @Select("SELECT file_name FROM files where file_id = #{fileId} AND (webdav_path IS NULL OR webdav_path != 'deleted') LIMIT 1")
    String getFileNameByFileId(String fileId);

    @Select("SELECT full_size FROM files where file_id = #{fileId} LIMIT 1")
    Long getFullSizeByFileId(String fileId);

    void updateUrl(String prefix);

    @Select("SELECT * FROM files WHERE webdav_path = #{path}")
    FileInfo getFileByWebdavPath(String path);

    @Select("SELECT * FROM files WHERE webdav_path LIKE #{path} || '%' ORDER BY id DESC")
    List<FileInfo> getFilesByPathPrefix(String path);

    @Select("SELECT * FROM files WHERE file_id = #{fileId}")
    FileInfo getFileByFileId(String fileId);

    @Delete("DELETE FROM files WHERE file_id = #{fileId}")
    void deleteFile(String fileId);

    @Delete("DELETE FROM files WHERE webdav_path LIKE CONCAT(#{path}, '%')")
    void deleteFileByWebDav(String path);

    @Update("UPDATE files SET download_url = #{file.downloadUrl}, upload_time = #{file.uploadTime}, size = #{file.size}, full_size = #{file.fullSize}, file_id = #{file.fileId} WHERE webdav_path = #{target}")
    void updateFileAttributeByWebDav(@Param("file") FileInfo file, @Param("target") String target);

    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir) VALUES (#{file.fileName}, #{file.downloadUrl}, #{file.uploadTime}, #{file.fileId}, #{file.size}, #{file.fullSize}, #{target}, #{file.dir})")
    void moveFile(@Param("file") FileInfo sourceFile, @Param("target") String target);

    @Update("UPDATE files SET is_public = #{isPublic} WHERE file_id = #{fileId}")
    void updateIsPublic(@Param("fileId") String fileId, @Param("isPublic") boolean isPublic);
}
