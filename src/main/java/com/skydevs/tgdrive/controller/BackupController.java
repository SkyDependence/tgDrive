package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.skydevs.tgdrive.annotation.NotEmptyFile;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

/**
 * Description:
 * 数据库备份功能
 * @author SkyDev
 * @date 2025-07-30 16:44:45
 */
@RestController
@RequestMapping("/api/backup")
@Slf4j
@RequiredArgsConstructor
public class BackupController {

    private static final String DATABASE_PATH = "db/tgDrive.db"; // SQLite 文件路径
    private final BackupService backupService;

    /**
     * Description:
     * 下载备份数据库文件
     * @author SkyDev
     * @date 2025-07-30 16:43:51
     * @return 数据库文件
     */
    @SaCheckRole("admin")
    @GetMapping("/download")
    public ResponseEntity<Resource> downloadBackup(){
        File file = new File(DATABASE_PATH);
        if (!file.exists()) {
            log.error("未找到数据库文件");
            return ResponseEntity.notFound().build();
        }

        log.info("name: " + file.getName() + " path: " + file.getAbsolutePath());

        if (!file.canRead()) {
            log.error("无权限读取数据库文件：{}", DATABASE_PATH);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tgDrive.db")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /**
     * Description:
     * 恢复数据库
     * @author SkyDev
     * @date 2025-07-30 16:42:50
     * @param multipartFile 数据库文件
     * @return 是否成功
     */
    @SaCheckRole("admin")
    @PostMapping("/upload")
    //TODO: 参数校验
    public Result<String> uploadBackupDb(@NotEmptyFile @RequestParam MultipartFile multipartFile) {
        String filename = multipartFile.getOriginalFilename();
        if (filename == null || !filename.endsWith(".db")) {
            log.warn("上传了不支持的文件类型: {}", filename);
            return Result.error("请上传.db文件");
        }

        try {
            backupService.loadBackupDb(multipartFile);
            log.info("数据库恢复成功");
            return Result.success("数据库恢复成功");
        } catch (Exception e) {
            log.error("恢复数据库失败");
            return Result.error("恢复数据库失败");
        }
    }
}
