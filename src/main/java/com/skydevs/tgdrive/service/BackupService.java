package com.skydevs.tgdrive.service;

import org.springframework.web.multipart.MultipartFile;

public interface BackupService {
    void loadBackupDb(MultipartFile db) throws Exception;
}
