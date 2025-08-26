package com.skydevs.tgdrive.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.skydevs.tgdrive.annotation.NotEmptyFile;
import com.skydevs.tgdrive.dto.CompleteUploadRequest;
import com.skydevs.tgdrive.dto.InitUploadRequest;
import com.skydevs.tgdrive.dto.UploadFile;
import com.skydevs.tgdrive.dto.UploadSessionResponse;
import com.skydevs.tgdrive.entity.UploadSession;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ChunkStorageService;
import com.skydevs.tgdrive.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkStorageService chunkStorageService;

    private final FileStorageService fileStorageService;

    /**
     * Description:
     * 初始化上传会话
     * @author SkyDev
     * @date 2025-08-26 14:46:06
     * @param request 初始化上传会话请求
     * @return 会话
     */
    @PostMapping("/init")
    public Result<UploadSessionResponse> initUpload(@Valid @RequestBody InitUploadRequest request) {
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            UploadSession session = chunkStorageService.createUploadSession(
                request.getFilename(), 
                request.getTotalSize(), 
                userId
            );

            UploadSessionResponse response = UploadSessionResponse.builder()
                .identifier(session.getIdentifier())
                .filename(session.getFilename())
                .totalSize(session.getTotalSize())
                .totalChunks(session.getTotalChunks())
                .chunkSize(session.getChunkSize())
                .status(session.getStatus())
                .uploadedChunks(List.of())
                .build();

            return Result.success(response);
        } catch (Exception e) {
            log.error("Failed to init upload", e);
            return Result.error("初始化上传失败: " + e.getMessage());
        }
    }

    /**
     * Description:
     * 检查已上传的分块
     * @author SkyDev
     * @date 2025-08-26 14:47:07
     * @param identifier 会话唯一标识
     * @return 已上传的分块集合
     */
    @GetMapping("/{identifier}/chunks")
    public Result<List<Integer>> getUploadedChunks(@NotBlank @PathVariable String identifier) {
        try {
            List<Integer> chunks = chunkStorageService.checkUploadedChunks(identifier).stream().sorted().toList();
            return Result.success(chunks);
        } catch (Exception e) {
            log.error("Failed to get uploaded chunks", e);
            return Result.error("获取已上传分块失败: " + e.getMessage());
        }
    }

    /**
     * 上传分块
     */
    /**
     * Description:
     *
     * @author SkyDev
     * @date 2025-08-26 14:47:47
     * @param identifier 会话唯一标识
     * @param chunkNumber 分块序号
     * @param file 分块文件
     * @param filename 分块文件名
     * @return
     */
    @PostMapping("/{identifier}/{chunkNumber}")
    public Result<?> uploadChunk(
        @NotBlank @PathVariable String identifier,
        @PathVariable Integer chunkNumber,
        @NotEmptyFile @RequestParam("file") MultipartFile file,
        @RequestParam(value = "filename", required = false) String filename
    ) {
        try {
            // Check if chunk already exists
            if (chunkStorageService.isChunkExists(identifier, chunkNumber)) {
                return Result.success("分块已存在");
            }

            // Upload chunk to Telegram
            String chunkFilename = filename != null ? filename : "chunk_" + chunkNumber;
            try (InputStream inputStream = file.getInputStream()) {
                chunkStorageService.saveChunkToTelegram(identifier, chunkNumber, inputStream, chunkFilename);
            }

            return Result.success();
        } catch (Exception e) {
            log.error("Failed to upload chunk: {} for identifier: {}", chunkNumber, identifier, e);
            return Result.error("上传分块失败: " + e.getMessage());
        }
    }

    /**
     * 完成上传
     */
    @PostMapping("/{identifier}/complete")
    public Result<UploadFile> completeUpload(
        @PathVariable String identifier,
        @RequestBody CompleteUploadRequest request
    ) {
        try {
            // Check if file already exists
            if (fileStorageService.isFileExists(identifier)) {
                return Result.error("文件已存在");
            }

            // Get current request
            HttpServletRequest httpRequest = ((ServletRequestAttributes) 
                RequestContextHolder.getRequestAttributes())
                .getRequest();

            Long userId = StpUtil.getLoginIdAsLong();

            // Merge chunks and upload to Telegram
            UploadFile result = fileStorageService.mergeFile(
                identifier,
                request.getFilename(),
                request.getTotalChunks(),
                request.getTotalSize(),
                httpRequest,
                userId
            );

            return Result.success(result);
        } catch (IOException e) {
            log.error("Failed to complete upload for identifier: {}", identifier, e);
            return Result.error("完成上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("Failed to complete upload for identifier: {}", identifier, e);
            return Result.error("完成上传失败: " + e.getMessage());
        }
    }

    /**
     * 取消上传
     */
    @DeleteMapping("/{identifier}")
    public Result<?> cancelUpload(@PathVariable String identifier) {
        try {
            chunkStorageService.cleanupChunks(identifier);
            return Result.success();
        } catch (Exception e) {
            log.error("Failed to cancel upload for identifier: {}", identifier, e);
            return Result.error("取消上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取上传会话信息
     */
    @GetMapping("/{identifier}/info")
    public Result<UploadSession> getSessionInfo(@PathVariable String identifier) {
        try {
            UploadSession session = chunkStorageService.getUploadSession(identifier);
            if (session == null) {
                return Result.error("上传会话不存在");
            }
            return Result.success(session);
        } catch (Exception e) {
            log.error("Failed to get session info for identifier: {}", identifier, e);
            return Result.error("获取会话信息失败: " + e.getMessage());
        }
    }
}