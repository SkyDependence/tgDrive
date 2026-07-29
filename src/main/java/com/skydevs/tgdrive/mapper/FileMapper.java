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
    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir, user_id, is_public) VALUES (#{fileName}, #{downloadUrl}, #{uploadTime}, #{fileId}, #{size}, #{fullSize}, #{webdavPath}, #{dir}, #{userId}, #{isPublic})")
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
            // 上传者展示：优先关联用户名；WebDAV 上传的文件 user_id 为空但有 webdav_path，统一显示为 "WebDAV"
            StringBuilder sql = new StringBuilder("SELECT f.*, " +
                    "CASE WHEN u.username IS NOT NULL THEN u.username " +
                    "WHEN f.webdav_path IS NOT NULL THEN 'WebDAV' " +
                    "END as uploader " +
                    "FROM files f LEFT JOIN users u ON f.user_id = u.id WHERE 1=1");

        // WebDAV 目录记录（dir=1）不混入 web 文件列表，避免被当作普通文件展示/删除
        sql.append(" AND f.dir = 0");

        // 关键词过滤（参数化，防止 SQL 注入）
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND f.file_name LIKE '%' || #{keyword} || '%'");
        }

            // 权限过滤（userId 参数化绑定）
            if ("admin".equals(role)) {
                // admin可以查看所有文件，不添加额外条件
            } else if ("admin_filter".equals(role)) {
                // admin按指定用户筛选文件
                sql.append(" AND f.user_id = #{userId}");
            } else if ("user".equals(role)) {
                // user 只能查看自己上传的文件（已废弃“公开文件”概念：
                // 下载靠不可枚举的 fileId 授权，列表只展示归属自己的文件，
                // 避免 WebDAV / 他人文件混入个人“我的文件”列表造成越权观感）
                sql.append(" AND f.user_id = #{userId}");
            } else if ("visitor".equals(role) || userId == null) {
                // visitor 或未登录用户没有归属文件，列表为空
                sql.append(" AND 1 = 0");
            }

            sql.append(" ORDER BY f.upload_time DESC");
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

    @Delete("DELETE FROM files WHERE webdav_path LIKE #{path} || '%'")
    void deleteFileByWebDav(String path);

    @Update("UPDATE files SET download_url = #{file.downloadUrl}, upload_time = #{file.uploadTime}, size = #{file.size}, full_size = #{file.fullSize}, file_id = #{file.fileId} WHERE webdav_path = #{target}")
    void updateFileAttributeByWebDav(@Param("file") FileInfo file, @Param("target") String target);

    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir) VALUES (#{file.fileName}, #{file.downloadUrl}, #{file.uploadTime}, #{file.fileId}, #{file.size}, #{file.fullSize}, #{target}, #{file.dir})")
    void moveFile(@Param("file") FileInfo sourceFile, @Param("target") String target);

    @Update("UPDATE files SET is_public = #{isPublic} WHERE file_id = #{fileId}")
    void updateIsPublic(@Param("fileId") String fileId, @Param("isPublic") boolean isPublic);

    /**
     * 按前缀整体移动目录及其所有子项，单条 UPDATE 完成，保留全部属性
     * （file_id/download_url/user_id/is_public 等均不丢失）
     * @param srcPrefix 源目录路径（必须以 / 结尾）
     * @param destPrefix 目标目录路径（必须以 / 结尾）
     */
    @Update("UPDATE files SET webdav_path = #{destPrefix} || substr(webdav_path, length(#{srcPrefix})+1) WHERE webdav_path LIKE #{srcPrefix} || '%'")
    int moveWebdavByPrefix(@Param("srcPrefix") String srcPrefix, @Param("destPrefix") String destPrefix);

    /**
     * 精确路径移动单个文件，保留全部属性
     */
    @Update("UPDATE files SET webdav_path = #{dest} WHERE webdav_path = #{src}")
    int moveWebdavExact(@Param("src") String src, @Param("dest") String dest);

    /**
     * 按前缀复制目录及其所有子项（file_id 共享同一 Telegram 文件，属性保留）
     * @param srcPrefix 源目录路径（必须以 / 结尾）
     * @param destPrefix 目标目录路径（必须以 / 结尾）
     */
    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir, user_id, is_public) " +
            "SELECT file_name, download_url, upload_time, file_id, size, full_size, #{destPrefix} || substr(webdav_path, length(#{srcPrefix})+1), dir, user_id, is_public " +
            "FROM files WHERE webdav_path LIKE #{srcPrefix} || '%'")
    int copyWebdavByPrefix(@Param("srcPrefix") String srcPrefix, @Param("destPrefix") String destPrefix);

    /**
     * 精确路径复制单个文件，保留属性
     */
    @Insert("INSERT INTO files (file_name, download_url, upload_time, file_id, size, full_size, webdav_path, dir, user_id, is_public) " +
            "SELECT file_name, download_url, upload_time, file_id, size, full_size, #{dest}, dir, user_id, is_public " +
            "FROM files WHERE webdav_path = #{src}")
    int copyWebdavExact(@Param("src") String src, @Param("dest") String dest);

    /**
     * 按路径更新文件名（用于 MOVE/COPY 后同步顶层项的显示名）
     */
    @Update("UPDATE files SET file_name = #{fileName} WHERE webdav_path = #{path}")
    int updateFileNameByPath(@Param("path") String path, @Param("fileName") String fileName);
}
