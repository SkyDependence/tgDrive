package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description:
 * 用于断点续传功能
 * 文件上传会话实体
 * @author SkyDev
 * @date 2025-08-26 14:16:11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSession {
    private Long id;
    private String identifier;
    private String filename;
    private Long totalSize;
    private Integer totalChunks;
    private Integer chunkSize;
    private Long userId;
    private String status;
    private Long createdAt;
    private Long updatedAt;
    private String telegramFileId;
    private String fileIdentifier;
}