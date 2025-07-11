package com.skydevs.tgdrive.controller;

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

@RestController
@RequestMapping("/api/backup")
@Slf4j
@RequiredArgsConstructor
public class BackupController {

    private static final String DATABASE_PATH = "db/tgDrive.db"; // SQLite 文件路径

    private final BackupService backupService;

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

    @PostMapping("/upload")
    public Result<String> uploadBackupDb(@RequestParam MultipartFile multipartFile) {
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
