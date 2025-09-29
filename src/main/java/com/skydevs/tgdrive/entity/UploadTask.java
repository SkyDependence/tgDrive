package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 上传任务实体，用于支持断点续传
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadTask {
    private String id;                  // 任务ID (fileHash_userId)
    private Long userId;                // 用户ID
    private String fileName;            // 文件名
    private Long fileSize;              // 文件大小
    private String fileHash;            // 文件SHA256哈希
    private Integer chunkSize;          // 分块大小
    private Integer totalChunks;        // 总分块数
    private String uploadedChunks;      // JSON数组记录已上传分块
    private String chunkFileIds;        // JSON对象记录分块fileId
    private String status;              // 任务状态
    private String errorMessage;        // 错误信息
    private String finalFileId;         // 最终文件ID
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
    private LocalDateTime expiresAt;    // 过期时间
}