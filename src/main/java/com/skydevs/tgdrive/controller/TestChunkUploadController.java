package com.skydevs.tgdrive.controller;

import com.skydevs.tgdrive.dto.ChunkUploadRequest;
import com.skydevs.tgdrive.entity.UploadSession;
import com.skydevs.tgdrive.result.Result;
import com.skydevs.tgdrive.service.ChunkStorageService;
import com.skydevs.tgdrive.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test controller for chunked upload functionality
 */
@Slf4j
@RestController
@RequestMapping("/api/test/chunk")
@RequiredArgsConstructor
public class TestChunkUploadController {

    private final ChunkStorageService chunkStorageService;
    private final FileStorageService fileStorageService;

    /**
     * Initialize upload session
     */
    @PostMapping("/init")
    public Result<Map<String, Object>> initUpload(
            @RequestParam String filename,
            @RequestParam Long totalSize,
            @RequestParam(required = false) Long userId) {
        
        try {
            UploadSession session = chunkStorageService.createUploadSession(
                filename, 
                totalSize, 
                userId != null ? userId : 1L
            );
            
            Map<String, Object> data = new HashMap<>();
            data.put("identifier", session.getIdentifier());
            data.put("chunkSize", session.getChunkSize());
            data.put("totalChunks", session.getTotalChunks());
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to initialize upload session", e);
            return Result.error("Failed to initialize upload session");
        }
    }

    /**
     * Upload a chunk
     */
    @PostMapping("/{identifier}/{chunkNumber}")
    public Result<String> uploadChunk(
            @PathVariable String identifier,
            @PathVariable Integer chunkNumber,
            @RequestParam("file") MultipartFile file,
            @RequestParam String filename) {
        
        try {
            String chunkId = ((com.skydevs.tgdrive.service.impl.ChunkStorageServiceImpl) chunkStorageService)
                .saveChunkToTelegram(identifier, chunkNumber, file.getInputStream(), filename);
            
            return Result.success(chunkId);
        } catch (IOException e) {
            log.error("Failed to upload chunk {}", chunkNumber, e);
            return Result.error("Failed to upload chunk: " + e.getMessage());
        }
    }

    /**
     * Check uploaded chunks
     */
    @GetMapping("/{identifier}/chunks")
    public Result<Set<Integer>> checkChunks(@PathVariable String identifier) {
        try {
            Set<Integer> chunks = chunkStorageService.checkUploadedChunks(identifier);
            return Result.success(chunks);
        } catch (Exception e) {
            log.error("Failed to check chunks", e);
            return Result.error("Failed to check chunks");
        }
    }

    /**
     * Complete upload
     */
    @PostMapping("/{identifier}/complete")
    public Result<Map<String, Object>> completeUpload(
            @PathVariable String identifier,
            @RequestParam String filename,
            @RequestParam Integer totalChunks,
            @RequestParam Long totalSize) {
        
        try {
            // This would normally be called through FileStorageService.mergeFile
            // For testing, we'll just return success
            Map<String, Object> data = new HashMap<>();
            data.put("message", "Upload completed successfully");
            data.put("identifier", identifier);
            
            return Result.success(data);
        } catch (Exception e) {
            log.error("Failed to complete upload", e);
            return Result.error("Failed to complete upload");
        }
    }

    /**
     * Get upload session info
     */
    @GetMapping("/{identifier}/info")
    public Result<UploadSession> getSessionInfo(@PathVariable String identifier) {
        try {
            UploadSession session = ((com.skydevs.tgdrive.service.impl.ChunkStorageServiceImpl) chunkStorageService)
                .getUploadSession(identifier);
            
            if (session != null) {
                return Result.success(session);
            } else {
                return Result.error("Session not found");
            }
        } catch (Exception e) {
            log.error("Failed to get session info", e);
            return Result.error("Failed to get session info");
        }
    }

    /**
     * Cleanup session
     */
    @DeleteMapping("/{identifier}")
    public Result<String> cleanupSession(@PathVariable String identifier) {
        try {
            chunkStorageService.cleanupChunks(identifier);
            return Result.success("Session cleaned up successfully");
        } catch (Exception e) {
            log.error("Failed to cleanup session", e);
            return Result.error("Failed to cleanup session");
        }
    }
}