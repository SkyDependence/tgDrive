package com.skydevs.tgdrive.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ChunkUploadRequest {
    private String identifier;
    private Integer chunkNumber;
    private Integer totalChunks;
    private Long totalSize;
    private String filename;
    private MultipartFile file;
}
