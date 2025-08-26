package com.skydevs.tgdrive.service.impl;

import com.skydevs.tgdrive.service.BackupService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Transactional(rollbackFor = Exception.class)
@Service
@RequiredArgsConstructor
public class BackupServiceImpl implements BackupService {

    private JdbcTemplate jdbcTemplate;
    private static final String BACKEND_DB_PATH = "jdbc:sqlite:db/tgDrive.db"; // 后端数据库路径
    private static final String BACKUP_DB_PATH = "db/tgdrive_backup.db"; // 备份路径

    @Override
    public void loadBackupDb(MultipartFile db) throws Exception {
        File tempFile = File.createTempFile("uploaded", ".db");

        // 连接 SQLite 数据库
        try {
            db.transferTo(tempFile.toPath());

            // 备份当前数据库
            Files.copy(Paths.get(BACKEND_DB_PATH.replace("jdbc:sqlite:", "")),
                    Paths.get(BACKUP_DB_PATH),
                    StandardCopyOption.REPLACE_EXISTING);

            // 使用 Statement 执行 ATTACH DATABASE
            String attachSql = "ATTACH DATABASE '" + tempFile.getAbsolutePath() + "' AS tempDb;";
            jdbcTemplate.execute(attachSql);
            // 合并 files 表数据
            String mergeSql = """
                INSERT INTO files (file_name, download_url, upload_time, size, full_size, file_id, webdav_path, dir)
                SELECT file_name, download_url, upload_time, size, full_size, file_id, webdav_path, dir
                FROM tempDb.files
                WHERE NOT EXISTS (SELECT 1 FROM files WHERE files.file_id = tempDb.files.file_id);
            """;
            jdbcTemplate.update(mergeSql);
        } finally {
            Files.deleteIfExists(tempFile.toPath()); // 删除临时文件
        }
    }
}
