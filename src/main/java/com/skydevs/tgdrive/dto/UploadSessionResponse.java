package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 上传会话响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadSessionResponse {
    private String identifier;
    private String filename;
    private Long totalSize;
    private Integer totalChunks;
    private Integer chunkSize;
    private String status;
    private List<Integer> uploadedChunks;
}