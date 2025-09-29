package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分块上传响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkUploadResponse {
    private String taskId;              // 任务ID
    private Integer chunkIndex;         // 分块索引
    private String chunkFileId;         // 分块在Telegram中的fileId
    private boolean success;            // 是否成功
    private String message;             // 消息
    private Integer uploadedChunksCount; // 已上传分块数量
    private Double progressPercentage;  // 总体进度百分比
}