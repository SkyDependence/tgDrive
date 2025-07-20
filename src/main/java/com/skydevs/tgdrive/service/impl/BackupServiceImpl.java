package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.service.BackupService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

@Service
public class BackupServiceImpl implements BackupService {

    private static final String BACKEND_DB_PATH = "jdbc:sqlite:db/tgDrive.db"; // 后端数据库路径
    private static final String BACKUP_DB_PATH = "db/tgdrive_backup.db"; // 备份路径

    @Override
    public void loadBackupDb(MultipartFile db) throws Exception {
        File tempFile = File.createTempFile("uploaded", ".db");
        db.transferTo(tempFile.toPath());

        // 备份当前数据库
        Files.copy(Paths.get(BACKEND_DB_PATH.replace("jdbc:sqlite:", "")),
                Paths.get(BACKUP_DB_PATH),
                StandardCopyOption.REPLACE_EXISTING);

        // 连接 SQLite 数据库
        try (Connection backendConn = DriverManager.getConnection(BACKEND_DB_PATH);
             Statement stmt = backendConn.createStatement()) {

            backendConn.setAutoCommit(false); // 开启事务

            // **使用 Statement 执行 ATTACH DATABASE**
            String attachSql = "ATTACH DATABASE '" + tempFile.getAbsolutePath() + "' AS tempDb;";
            stmt.execute(attachSql);

            // **合并 files 表数据**
            String mergeSql = """
                INSERT INTO files (file_name, download_url, upload_time, size, full_size, file_id, webdav_path, dir)
                SELECT file_name, download_url, upload_time, size, full_size, file_id, webdav_path, dir
                FROM tempDb.files
                WHERE NOT EXISTS (SELECT 1 FROM files WHERE files.file_id = tempDb.files.file_id);
            """;
            stmt.executeUpdate(mergeSql);

            backendConn.commit(); // 提交事务
        } catch (Exception e) {
            throw new RuntimeException("数据库导入失败", e);
        } finally {
            Files.deleteIfExists(tempFile.toPath()); // 删除临时文件
        }
    }
}
