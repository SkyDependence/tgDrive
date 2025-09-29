package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 上传准备响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadPrepareResponse {
    private String taskId;              // 上传任务ID
    private boolean resumable;          // 是否可续传
    private boolean completed;          // 是否已完成（秒传）
    private Integer totalChunks;        // 总分块数
    private List<Integer> uploadedChunks; // 已上传的分块索引
    private Integer chunkSize;          // 分块大小
    private String finalFileId;         // 如果已完成，返回文件ID
    private String downloadUrl;         // 如果已完成，返回下载链接

    // 用于前端计算的辅助信息
    private Long uploadedSize;          // 已上传大小
    private Double uploadProgress;      // 上传进度百分比
}