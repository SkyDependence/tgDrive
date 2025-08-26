package com.skydevs.tgdrive.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件分块实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileChunk {
    private Long id;
    private Long sessionId;
    private Integer chunkNumber;
    private String chunkId;
    private Integer chunkSize;
    private String status;
    private Long createdAt;
}