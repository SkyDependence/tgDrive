package com.skydevs.tgdrive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 完成上传请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteUploadRequest {
    private String identifier;
    private String filename;
    private Integer totalChunks;
    private Long totalSize;
}