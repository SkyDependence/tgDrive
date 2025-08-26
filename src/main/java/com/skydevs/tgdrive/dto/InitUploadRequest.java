package com.skydevs.tgdrive.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 初始化上传请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitUploadRequest {
    @NotBlank
    private String filename;
    @NotNull
    private Long totalSize;
    private Long userId;
}