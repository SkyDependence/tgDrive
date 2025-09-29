package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 上传任务DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadTaskDTO {
    private String id;                  // 任务ID
    private String fileName;            // 文件名
    private Long fileSize;              // 文件大小
    private String fileSizeStr;         // 文件大小（人类可读）
    private Integer totalChunks;        // 总分块数
    private Integer uploadedChunks;     // 已上传分块数
    private Double progress;            // 上传进度（百分比）
    private String status;              // 任务状态
    private String statusText;          // 状态文本（中文）
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
    private LocalDateTime expiresAt;    // 过期时间
    private String errorMessage;        // 错误信息
    private boolean resumable;          // 是否可恢复
    private Long remainingSize;         // 剩余大小
    private String remainingSizeStr;    // 剩余大小（人类可读）
}